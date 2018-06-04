package com.thomaskioko.livedatademo.repository.util;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.thomaskioko.livedatademo.repository.model.ApiResponse;
import com.thomaskioko.livedatademo.utils.Objects;
import com.thomaskioko.livedatademo.vo.Resource;


/**
 * A generic class that can provide a resource backed by both the sqlite database and the network.
 * <p>
 * You can read more about it in the <a href="https://developer.android.com/arch">Architecture
 * Guide</a>.
 * NetworkBoundResource的设计意图：在单一数据源原则下设计的数据加载帮助类(数据库/网络)
 *一个类的行为应该由决策树来指导，当一个类的决策树确定时那么该类也就设计完成了，剩下只需要将逻辑填充完整即可
 * @param <ResultType>
 * @param <RequestType>
 */
public abstract class NetworkBoundResource<ResultType, RequestType> {
    private final AppExecutors appExecutors;
/*
有十几种不同的特定变换可能在你的应用中有用，但默认情况不提供它们。要实现你自己的变换，你可以使用MediatorLiveData类，
该类专门用于正确监听别的LiveData实例，并处理它们发出的事件。MediatorLiveData负责将其活动/非活动的状态正确
传递到源LiveData。
故名思议，MediatorLiveData的用法就是将一种数据如果可以的话拆分成我们想要的数据并跟不同控件进行绑定。
当然，如果你嫌弃用MediatorLiveData麻烦的话可以直接用Transformations这个工具类，
它直接提供了map和switchMap两个方法帮你返回MediatorLiveData对象。
 */


    // 外围一直观察者这个result； UI<----result<-----dbSource/apiResponse
    private final MediatorLiveData<Resource<ResultType>> result = new MediatorLiveData<>();

    // 标注该方法只能在主线程中使用
    @MainThread
    public NetworkBoundResource(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
        result.setValue(Resource.loading(null)); // ----显示正在加载
        //这里demo先从db加载，无数据在从网络加载
        //TODO:: Add method to check if data should be saved. This should apply for search data.
        LiveData<ResultType> dbSource = loadFromDb();
        // 加入对db返回数据的观察
        result.addSource(dbSource, data -> {
            // 对dbSource数据变化处理不一样(判断是需要从网络获取)，故这里移除观察
            result.removeSource(dbSource);
            if (shouldFetch(data)) {//从网络获取数据
                fetchFromNetwork(dbSource);
            } else { //不从网络获取数据
                // 对dbSource数据变化处理不一样(观察db数据源数据变化，则更新result)，故这里重新观察
                // 这里的setValue，是当前类的setValue；
                // 为什么这里获取到了结果了，还要观察db数据源呢？比较巧妙：作用理由首次addSource触发onchange方法，dbSource的数据设置到result
                result.addSource(dbSource, newData -> setValue(Resource.success(newData)));
            }
        });
    }

    //标注该方法只能在主线程中使用
    @MainThread
    private void setValue(Resource<ResultType> newValue) {
        if (!Objects.equals(result.getValue(), newValue)) {
            result.setValue(newValue);
        }
    }

    // 从网络加载 （里清楚如何显示加载中，网络异常，数据返回； 如何更新缓存）
    private void fetchFromNetwork(final LiveData<ResultType> dbSource) {
        LiveData<ApiResponse<RequestType>> apiResponse = createCall();
        // 再次对db数据源和api数据源进行观察，为什么网络的，还要对db数据源进行观察呢？这个应该是多余的
        // we re-attach dbSource as a new source, it will dispatch its latest value quickly
        result.addSource(dbSource, newData -> setValue(Resource.loading(newData)));
        result.addSource(apiResponse, response -> {
            // 谁有变动了，取消观察，处理数据
            result.removeSource(apiResponse);
            result.removeSource(dbSource);
            //noinspection ConstantConditions
            if (response.isSuccessful()) {
                appExecutors.diskIO().execute(() -> {
                    //保存到数据库,重新加载一次数据
                    saveCallResult(processResponse(response));
                    appExecutors.mainThread().execute(() ->
                            // we specially request a new live data,
                            // otherwise we will get immediately last cached value,
                            // which may not be updated with latest results received from network.
                            result.addSource(loadFromDb(),
                                    newData -> setValue(Resource.success(newData)))
                    );
                });
            } else {
                onFetchFailed();
                // 比较巧妙：作用理由首次addSource触发onchange方法，dbSource的数据设置到result
                result.addSource(dbSource,
                        newData -> setValue(Resource.error(response.errorMessage, newData)));
            }
        });
    }

    protected void onFetchFailed() {
    }

    public LiveData<Resource<ResultType>> asLiveData() {
        return result;
    }

    @WorkerThread
    protected RequestType processResponse(ApiResponse<RequestType> response) {
        return response.body;
    }

    // 当要把网络数据存储到数据库中时调用
    @WorkerThread
    protected abstract void saveCallResult(@NonNull RequestType item);

    // 决定是否去网络获取数据
    @MainThread
    protected abstract boolean shouldFetch(@Nullable ResultType data);

    // 用于从数据库中获取缓存数据
    @NonNull
    @MainThread
    protected abstract LiveData<ResultType> loadFromDb();

    // 创建网络数据请求

    @NonNull
    @MainThread
    protected abstract LiveData<ApiResponse<RequestType>> createCall();
}
