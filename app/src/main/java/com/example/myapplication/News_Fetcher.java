package com.example.myapplication;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kwabenaberko.newsapilib.NewsApiClient;
import com.kwabenaberko.newsapilib.models.Article;
import com.kwabenaberko.newsapilib.models.request.EverythingRequest;
import com.kwabenaberko.newsapilib.models.response.ArticleResponse;

import java.util.ArrayList;

public class News_Fetcher extends ViewModel {
    private final String TAG = "NEWS_FETCHER";
    private final String API_KEY = "de7b5e27076d48329b616ffbf816543e";
    private final NewsApiClient newsApiClient = new NewsApiClient(API_KEY);

    // 1. Private, writable holder for the data
    private final MutableLiveData<ArrayList<Article>> articles = new MutableLiveData<>();
    private final MutableLiveData<Boolean> error = new MutableLiveData<>();

    // 2. Public, read-only view for the UI
    public LiveData<ArrayList<Article>> getArticles() { return articles; }
    public LiveData<Boolean> getError() { return error; }

    public News_Fetcher() {

    }

    public void fetchArticles(String query) {
        error.setValue(null);
        newsApiClient.getEverything(
                new EverythingRequest.Builder()
                        .q(query)
                        .language("en")
                        .pageSize(100)
                        .build(),
                new NewsApiClient.ArticlesResponseCallback() {
                    @Override
                    public void onSuccess(ArticleResponse response) {
                        if (response == null || response.getArticles() == null) {
                            error.postValue(true);
                            return;
                        }
                        ArrayList<Article> validArticles = new ArrayList<>();
                        for (Article article : response.getArticles()) {
                            if (article != null && ArticleView.isArticleUrl(article.getUrl())) {
                                validArticles.add(article);
                            }
                        }
                        articles.postValue(validArticles);
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Log.d(TAG, "news api failure", e);
                        error.postValue(true);
                    }
                });
    }
}
