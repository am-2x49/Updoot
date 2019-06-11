package com.ducktapedapps.updoot.viewModels;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.ducktapedapps.updoot.model.LinkData;
import com.ducktapedapps.updoot.model.ListingData;
import com.ducktapedapps.updoot.model.thing;
import com.ducktapedapps.updoot.repository.submissionsRepo;
import com.ducktapedapps.updoot.utils.constants;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class submissionsVM extends AndroidViewModel {

    private CompositeDisposable disposable = new CompositeDisposable();
    private static final String TAG = "submissionsVM";
    private submissionsRepo frontPageRepo;

    private MutableLiveData<String> sorting = new MutableLiveData<>();
    private MutableLiveData<List<thing>> allSubmissions = new MutableLiveData<>();
    private MutableLiveData<String> state = new MutableLiveData<>();

    public MutableLiveData<String> getSorting() {
        return sorting;
    }

    public MutableLiveData<List<thing>> getAllSubmissions() {
        return allSubmissions;
    }

    public MutableLiveData<String> getState() {
        return state;
    }

    public submissionsVM(Application application) {
        super(application);
        frontPageRepo = new submissionsRepo(application);

        sorting.setValue(constants.TOP);
        loadNextPage();
    }

    public void loadNextPage() {
        disposable.add(
                frontPageRepo
                        .loadNextPage(getSorting().getValue())
                        .map(thing -> {
                            if (thing.getData() instanceof ListingData) {
                                return ((ListingData) thing.getData()).getChildren();
                            } else {
                                throw new Exception("unsupported response");
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .doOnSubscribe(__ -> {
                            Log.i(TAG, "onSubscribe: ");
                            state.postValue(constants.LOADING_STATE);
                        })
                        .subscribe(submissions -> {
                            Log.i(TAG, "onSuccess: " + submissions.size());
                            List<thing> newList = allSubmissions.getValue();
                            if (newList == null) {
                                newList = new ArrayList<>();
                            }
                            newList.addAll(submissions);
                            allSubmissions.postValue(newList);
                            state.postValue(constants.SUCCESS_STATE);
                        }, throwable -> {
                            Log.e(TAG, "onError: ", throwable.getCause());
                            state.postValue(throwable.getMessage());
                        }));
    }

    public void reload(String sort) {
        frontPageRepo.setAfter(null);
        allSubmissions.setValue(null);
        sorting.setValue(sort);
        loadNextPage();

    }

    @Override
    protected void onCleared() {
        if (!disposable.isDisposed()) {
            disposable.clear();
        }
        super.onCleared();
        Log.i(TAG, "onCleared: ");
    }
}
