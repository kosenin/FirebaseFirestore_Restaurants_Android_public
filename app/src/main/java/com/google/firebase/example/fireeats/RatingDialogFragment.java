package com.google.firebase.example.fireeats;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.example.fireeats.model.Rating;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

/**
 * Диалоговый фрагменты содержащий форму для заполнения рейтинга
 */
public class RatingDialogFragment extends DialogFragment {

    public static final String TAG = "RatingDialog";

    // связываем переменные с UI
    @BindView(R.id.restaurant_form_rating)
    MaterialRatingBar mRatingBar;

    @BindView(R.id.restaurant_form_text)
    EditText mRatingText;

    // интерфейс для передачи информации в RestaurantDetailActivity в случае написания рейтинга пользователем
    interface RatingListener {

        void onRating(Rating rating);

    }
    // "слушатель" (интерфейс, который имплементирует RestaurantDetailActivity )
    private RatingListener mRatingListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // "Надуваем" разметку
        View v = inflater.inflate(R.layout.dialog_rating, container, false);
        // запускаем Butterknife (библиотека для связывания UI с переменными)
        ButterKnife.bind(this, v);

        return v;
    }

    // Прикрепляем инстанцию интерфейса (это по сути RestaurantDetailActivity) для передачи данных
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof RatingListener) {
            mRatingListener = (RatingListener) context;
        }
    }

    // устанавливаем layout и его параметры
    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    // в случае клика на кнопку заполнения рейтинга передаем информацию в RestaurantDetailActivity
    @OnClick(R.id.restaurant_form_button)
    public void onSubmitClicked(View view) {
        Rating rating = new Rating(
                FirebaseAuth.getInstance().getCurrentUser(),
                mRatingBar.getRating(),
                mRatingText.getText().toString());

        if (mRatingListener != null) {
            mRatingListener.onRating(rating);
        }
        // закрываем диалоговое окно
        dismiss();
    }

    // Устанавливаем действия на кнопку (выход из диалогового окна)
    @OnClick(R.id.restaurant_form_cancel)
    public void onCancelClicked(View view) {
        dismiss();
    }
}
