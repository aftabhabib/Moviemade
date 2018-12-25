package org.michaelbel.moviemade.ui.modules.similar;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.jetbrains.annotations.NotNull;
import org.michaelbel.moviemade.BuildConfig;
import org.michaelbel.moviemade.Moviemade;
import org.michaelbel.moviemade.R;
import org.michaelbel.moviemade.data.entity.Movie;
import org.michaelbel.moviemade.ui.GridSpacingItemDecoration;
import org.michaelbel.moviemade.ui.base.BaseFragment;
import org.michaelbel.moviemade.ui.modules.main.adapter.MoviesAdapter;
import org.michaelbel.moviemade.ui.modules.main.adapter.OnMovieClickListener;
import org.michaelbel.moviemade.ui.receivers.NetworkChangeListener;
import org.michaelbel.moviemade.ui.receivers.NetworkChangeReceiver;
import org.michaelbel.moviemade.ui.widgets.EmptyView;
import org.michaelbel.moviemade.utils.DeviceUtil;
import org.michaelbel.moviemade.utils.IntentsKt;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;

public class SimilarMoviesFragment extends BaseFragment implements SimilarContract.View, NetworkChangeListener, OnMovieClickListener {

    private int movieId;
    private SimilarMoviesActivity activity;
    private MoviesAdapter adapter;
    private GridLayoutManager gridLayoutManager;

    private NetworkChangeReceiver networkChangeReceiver;
    private boolean connectionFailure = false;

    @Inject SimilarContract.Presenter presenter;
    @Inject SharedPreferences sharedPreferences;

    @BindView(R.id.empty_view) EmptyView emptyView;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;

    public static SimilarMoviesFragment newInstance(int movieId) {
        Bundle args = new Bundle();
        args.putInt(IntentsKt.MOVIE_ID, movieId);

        SimilarMoviesFragment fragment = new SimilarMoviesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (SimilarMoviesActivity) getActivity();

        networkChangeReceiver = new NetworkChangeReceiver(this);
        activity.registerReceiver(networkChangeReceiver, new IntentFilter(NetworkChangeReceiver.INTENT_ACTION));

        Moviemade.get(activity).getFragmentComponent().inject(this);
        presenter.setView(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movies, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity.getToolbar().setOnClickListener(v -> recyclerView.smoothScrollToPosition(0));

        int spanCount = activity.getResources().getInteger(R.integer.movies_span_layout_count);

        adapter = new MoviesAdapter(this);
        gridLayoutManager = new GridLayoutManager(activity, spanCount);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(spanCount, DeviceUtil.INSTANCE.dp(activity, 3)));
        //recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(activity, R.anim.layout_animation_from_bottom));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1) && adapter.getItemCount() != 0) {
                    presenter.getSimilarMoviesNext(movieId);
                }
            }
        });

        emptyView.setOnClickListener(v -> presenter.getSimilarMovies(movieId));

        movieId = getArguments() != null ? getArguments().getInt(IntentsKt.MOVIE_ID) : 0;
        presenter.getSimilarMovies(movieId);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        int spanCount = activity.getResources().getInteger(R.integer.movies_span_layout_count);
        Parcelable state = gridLayoutManager.onSaveInstanceState();
        gridLayoutManager = new GridLayoutManager(activity, spanCount, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        gridLayoutManager.onRestoreInstanceState(state);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity.unregisterReceiver(networkChangeReceiver);
        presenter.onDestroy();
    }

    @Override
    public void setMovies(@NotNull List<Movie> movies) {
        connectionFailure = false;
        progressBar.setVisibility(View.GONE);
        adapter.addMovies(movies);
    }

    @Override
    public void setError(int mode) {
        connectionFailure = false;
        progressBar.setVisibility(View.GONE);
        emptyView.setMode(mode);

        // noinspection ConstantConditions, StringEquality
        if (BuildConfig.TMDB_API_KEY == "null") {
            emptyView.setValue(R.string.error_empty_api_key);
        }
    }

    @Override
    public void onNetworkChanged() {
        if (connectionFailure && adapter.getItemCount() == 0) {
            presenter.getSimilarMovies(movieId);
        }
    }

    @Override
    public void onMovieClick(@NotNull Movie movie, @NotNull View view) {
        activity.startMovie(movie);
    }
}