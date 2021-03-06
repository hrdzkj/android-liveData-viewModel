package com.thomaskioko.livedatademo.view.ui.fragment;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.thomaskioko.livedatademo.R;
import com.thomaskioko.livedatademo.db.entity.Movie;
import com.thomaskioko.livedatademo.di.Injectable;
import com.thomaskioko.livedatademo.utils.DeviceUtils;
import com.thomaskioko.livedatademo.view.adapter.MovieListAdapter;
import com.thomaskioko.livedatademo.view.adapter.SearchItemAdapter;
import com.thomaskioko.livedatademo.view.ui.common.NavigationController;
import com.thomaskioko.livedatademo.viewmodel.MovieListViewModel;
import com.thomaskioko.livedatademo.vo.Resource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MovieListFragment extends Fragment implements Injectable {

    @Inject  // 注解了@Inject的变量表示该变量要求Dagger为其注入值
    public ViewModelProvider.Factory viewModelFactory;
    @Inject
    public NavigationController navigationController;

    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.search_results)
    RecyclerView searchResultsRecyclerView;
    @BindView(R.id.progressbar)
    ProgressBar progressBar;
    @BindView(R.id.tvError)
    TextView errorTextView;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.search_view)
    MaterialSearchView materialSearchView;

    private MovieListViewModel mMovieListViewModel;
    private MovieListAdapter mMovieListAdapter;
    private SearchItemAdapter searchAdapter;
    private List<Movie> mMovieList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.movie_list_fragment, container, false);
        ButterKnife.bind(this, view);
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
        setHasOptionsMenu(true);

        DeviceUtils.setTranslucentStatusBar(getActivity().getWindow(), R.color.colorPrimaryDark);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(mRecyclerView.getContext(), 3);
        mRecyclerView.setLayoutManager(gridLayoutManager);

        mMovieListAdapter = new MovieListAdapter(
                (ivPoster, movie) -> navigationController.navigateToMovieDetailFragment(ivPoster, movie.id)
        );
        mRecyclerView.setAdapter(mMovieListAdapter);

        /*
        当Activity被重构时，获得到的model实例是与重构前同一个，当MyActivity被销毁时，
        Framework会调用ViewModel的onCleared(),我们就可以在此方法中做资源的清理。
        因为ViewModel的生命周期是和Activity或Fragment分开的，所以在ViewModel中绝对不能引用任何View对象
        或者任何引用了Activity的Context的对象。如果ViewModel中需要Application的Context的话，可以继承
        AndroidViewModel;
        ViewModelProviders.of(getActivity()).get(SharedViewModel.class)注意的是两个Fragment都使用了getActivity作为参数来获得ViewModel实例。
        这表示这两个Fragment获得的ViewModel对象是同一个。这可以用来处理fragment间的通信。
        viewModel的生命周期：只有当Activity的finish()方法被调用时，ViewModel.onCleared()方法会被调用，对象才会被销毁。
        https://developer.android.google.cn/images/topic/libraries/architecture/viewmodel-lifecycle.png
        基于此，viewModel可用用以存储持久化的用户信息，不必要存储所有列表信息，要存存最后一页即可。
        */
        mMovieListViewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(MovieListViewModel.class);

        mMovieListViewModel.getPopularMovies()
                .observe(this, this::handleResponse);

        mMovieListViewModel.getSearchResults()
                .observe(this, this::handleSearchResponse);
/*
observe()方法的第一个参数为LifecycleOwner， 表示Observer应当绑定Lifecycle， 这样意味着：
Lifecycle未处于STARTED或RESUMED状态， 即使该值发生了变化， Observer不会被通知。
如果Lifecycle被杀掉， Observer将被自动清除。----好巧妙
 */

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false
        );
        searchResultsRecyclerView.setLayoutManager(linearLayoutManager);

        searchAdapter = new SearchItemAdapter(
                (sharedImageView, movie) -> navigationController.navigateToMovieDetailFragment(sharedImageView, movie.id)
        );
        searchResultsRecyclerView.setAdapter(searchAdapter);

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);

        // Retrieve the SearchView and plug it into SearchManager
        MenuItem item = menu.findItem(R.id.action_search);
        materialSearchView.setMenuItem(item);
        materialSearchView.setCursorDrawable(R.drawable.color_cursor_white);

        materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                findMovie(query);
                dismissKeyboard(materialSearchView.getWindowToken());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                findMovie(newText);
                return false;
            }
        });

        materialSearchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                searchResultsRecyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSearchViewClosed() {
                searchResultsRecyclerView.setVisibility(View.GONE);
            }
        });

    }

    private void findMovie(String query) {
        if (!query.isEmpty() || !query.equals("")) {
            final List<Movie> filteredModelList = filter(mMovieList, query);
            if (filteredModelList.size() <= 0) {
                mMovieListViewModel.setSearchQuery(query);
            } else {
                searchAdapter.setItems(filteredModelList);
                searchAdapter.notifyDataSetChanged();
            }
        }

    }

    private List<Movie> filter(List<Movie> models, String query) {
        query = query.toLowerCase();
        final List<Movie> filteredModelList = new ArrayList<>();
        for (Movie model : models) {
            final String text = model.title.toLowerCase();
            if (text.contains(query)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    private void handleResponse(Resource<List<Movie>> listResource) {
        if (listResource != null) {
            switch (listResource.status) {
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText(listResource.message);
                    break;
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    errorTextView.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    errorTextView.setVisibility(View.GONE);
                    if (listResource.data != null && listResource.data.size() > 0) {
                        mMovieList = listResource.data;
                        mMovieListAdapter.setData(listResource.data);
                        mMovieListAdapter.notifyDataSetChanged();
                    } else {
                        errorTextView.setText(getResources().getString(R.string.error_no_results));
                        errorTextView.setVisibility(View.VISIBLE);
                    }
                    break;
                default:
                    progressBar.setVisibility(View.GONE);
                    errorTextView.setText(getResources().getString(R.string.error_no_results));
                    break;
            }
        }
    }

    private void handleSearchResponse(Resource<List<Movie>> listResource) {
        if (listResource != null) {
            switch (listResource.status) {
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    errorTextView.setText(listResource.message);
                    break;
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    errorTextView.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    errorTextView.setVisibility(View.GONE);
                    if (listResource.data != null && listResource.data.size() > 0) {
                        //一页数据是这样处理，多页数据也是同理，这样处理呀
                        searchAdapter.setItems(listResource.data);
                        searchAdapter.notifyDataSetChanged();
                    } else {
                        errorTextView.setText(getResources().getString(R.string.error_no_results));
                        errorTextView.setVisibility(View.VISIBLE);
                    }
                    break;
                default:
                    progressBar.setVisibility(View.GONE);
                    errorTextView.setText(getResources().getString(R.string.error_no_results));
                    break;
            }
        }
    }

    private void dismissKeyboard(IBinder windowToken) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(windowToken, 0);
            }
        }
    }

}
