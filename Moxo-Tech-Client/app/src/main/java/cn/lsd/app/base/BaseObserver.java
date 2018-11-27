package cn.lsd.app.base;

import android.content.Context;

import cn.lsd.app.api.exception.ApiErrorHelper;

import rx.Observer;


public class BaseObserver<T> implements Observer<T> {
    private Context mContext;

    public BaseObserver() {

    }

    public BaseObserver(Context context) {
        mContext = context;
    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
        ApiErrorHelper.handleCommonError(mContext, e);
    }

    @Override
    public void onNext(T t) {

    }
}
