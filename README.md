Android LiveData & ViewModel Demo
---------------------------------------
[![CircleCI](https://circleci.com/gh/kioko/android-liveData-viewModel.svg)](https://circleci.com/gh/kioko/android-liveData-viewModel)

A simple android project that demonstrates how to implement Android Architecture Components.

 <table>
  <td>
    <p align="center">
  <img src="https://github.com/kioko/android-liveData-viewModel/blob/master/art/HomeScreen.png?raw=true" alt="Home Page" width="250"/>
</p>
</td>
<td>
    <p align="center">
  <img src="https://github.com/kioko/android-liveData-viewModel/blob/master/art/MovieDetails.png?raw=true" alt="Movie Details" width="250"/>
    </p>
  </td>

</table>

### Architecture
The app uses ViewModel to abstract the data from UI and TmdbRepository as single source of truth for data. TmdbRepository first fetch the data from database if exist than display data to the user and at the same time it also fetches data from the webservice and update the result in database and reflect the changes to UI from database.

![](https://github.com/kioko/android-liveData-viewModel/blob/master/art/archtiture.png)

### Requirements

* JDK Version 1.7 & above
* Android Studio Preview Version 3.0

### Prerequisites
For the app to make requests you require a [TMDB API key](https://developers.themoviedb.org/3/getting-started ).

If you don’t already have an account, you will need to [create one](https://www.themoviedb.org/account/signup)
in order to request an API Key.

Once you have it, open `gradle.properties` file and paste your API key in `TMDB_API_KEY` variable.
(已经在网上找到了一个可用的key)
### Libraries


* [Android Support Library][support-lib]
* [Android Architecture Components][arch]
* [Dagger 2][dagger2] for dependency injection
* [Retrofit][retrofit] for REST api communication
* [OkHttp][OkHttp] for adding interceptors to Retrofit
* [Glide][glide] for image loading
* [Timber][timber] for logging
* [espresso][espresso] for UI tests
* [mockito][mockito] for mocking in tests


[mockwebserver]: https://github.com/square/okhttp/tree/master/mockwebserver
[support-lib]: https://developer.android.com/topic/libraries/support-library/index.html
[arch]: https://developer.android.com/arch
[OkHttp]: http://square.github.io/okhttp/
[espresso]: https://google.github.io/android-testing-support-library/docs/espresso/
[dagger2]: https://google.github.io/dagger
[retrofit]: http://square.github.io/retrofit
[glide]: https://github.com/bumptech/glide
[timber]: https://github.com/JakeWharton/timber
[mockito]: http://site.mockito.org


### License

    Copyright 2017 Thomas Kioko


    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    
    
    测试接口地址
    https://developers.themoviedb.org/3/movies/get-popular-movies


 ### 新增
生成ViewHolder的方式   mMovieListViewModel = ViewModelProviders.of(this, viewModelFactory).get(MovieListViewModel.class);
LiveData.observe 以被观察者订阅,可以以lambda形式定义Observer，LiveData什么数据参数就是什么数据。

如何通知数据改变呢？ 可能是要调用setValue（主线程）,postValue（子线程）才会调用，是protected方法，继承实现自己的LiveData可以使用。
网络demo:根据 mMovieListAdapter.setData(listResource.data)，observe前已经先建立好了LiveData,只负责知道有数据变化，
目前项目，用这个可以满足要求。


LiveData有onActive()和onInactive()方法，
看看类关系图class_livedata，可以知道有什么方法。
mMovieListViewModel.getPopularMovies().observe(this, this::handleResponse);
实际上可以参考LiveData来自己实现业务上的一些观察者模式。

学习：常见的错误就是把所有代码都写在Activity或者Fragment中。任何跟UI和系统交互无关的事情都不应该放在这些类当中。
FragmentManager 都另外封装了。NavigationController

-----------------
Lifecycle：是一个保存了一个组件当前状态的类，并且其他对象可以监听状态的变化。
定义了Event枚举类型和State枚举类型来追踪组件的状态. LifecycleObserver订阅了Lifecycle，在生命周期方法到来时
就会收到通知。

LifecycleOwner，发现它只是一个接口，里面只有一个getLifecycle方法需要实现。再是版本的Activity,fragment已经实现了该接口。

LiveData.observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer)
getLifecycle().addObserver(@NonNull LifecycleObserver observer);
关系：只有当 Observer 的 Lifecycle 对象处于 STARTED 或者 RESUMED 状态的时候，
LiveData 才处于活动状态，只有在活动状态数据变化事件才会通知到 Observer。

-------------
数据变换
LiveData 还支持简单的数据变换。目前在 Transformations 类中有 map 和 switchMap 两个变换函数，
如果熟悉RxJava 则对这两个函数应该不陌生：
map 是把一个数据类型变换为另外一个数据类型。
switchMap 是把一个数据变化为另外一个 LiveData

// 期间引出了好多框架
不知道是否稳定了的：android paging library
稳定的：mock，drag2
其他的：https://github.com/googlesamples/android-architecture-components

