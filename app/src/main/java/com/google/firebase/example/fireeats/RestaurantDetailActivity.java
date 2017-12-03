package com.google.firebase.example.fireeats;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.example.fireeats.adapter.RatingAdapter;
import com.google.firebase.example.fireeats.model.Rating;
import com.google.firebase.example.fireeats.model.Restaurant;
import com.google.firebase.example.fireeats.util.RestaurantUtil;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

/**
 * Activity для отображения детальной информации о ресторане (включая рейтинги) пользователю
 */
public class RestaurantDetailActivity extends AppCompatActivity
        implements EventListener<DocumentSnapshot>, RatingDialogFragment.RatingListener {

    // константы
    private static final String TAG = "RestaurantDetail";
    public static final String KEY_RESTAURANT_ID = "key_restaurant_id";

    // связываем переменные с UI
    @BindView(R.id.restaurant_image)
    ImageView mImageView;

    @BindView(R.id.restaurant_name)
    TextView mNameView;

    @BindView(R.id.restaurant_rating)
    MaterialRatingBar mRatingIndicator;

    @BindView(R.id.restaurant_num_ratings)
    TextView mNumRatingsView;

    @BindView(R.id.restaurant_city)
    TextView mCityView;

    @BindView(R.id.restaurant_category)
    TextView mCategoryView;

    @BindView(R.id.restaurant_price)
    TextView mPriceView;

    @BindView(R.id.view_empty_ratings)
    ViewGroup mEmptyView;

    @BindView(R.id.recycler_ratings)
    RecyclerView mRatingsRecycler;

    // переменные Firebase
    private FirebaseFirestore mFirestore;
    private DocumentReference mRestaurantRef;
    private RatingAdapter mRatingAdapter;

    // диалог и адаптер
    private RatingDialogFragment mRatingDialog;
    private ListenerRegistration mRestaurantRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Инициализируем UI
        super.onCreate(savedInstanceState);
        // "Надуваем" разметку
        setContentView(R.layout.activity_restaurant_detail);
        // запускаем Butterknife (библиотека для связывания UI с переменными)
        ButterKnife.bind(this);

        // Get restaurant ID from extras
        // Берем ID ресторана из переданных из MainActivity данных (метод onRestaurantSelected()
        String restaurantId = getIntent().getExtras().getString(KEY_RESTAURANT_ID);
        if (restaurantId == null) {
            throw new IllegalArgumentException("Must pass extra " + KEY_RESTAURANT_ID);
        }

        // Инициализируем Firestore
        mFirestore = FirebaseFirestore.getInstance();

        // Создаем ссылку на ресторан в базе данных Firestore
        mRestaurantRef = mFirestore.collection("restaurants").document(restaurantId);

        // Создаем запрос на рейтинги
        Query ratingsQuery = mRestaurantRef
                .collection("ratings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50);

        // RecyclerView
        // RecyclerView
        // создаем RatingAdapter, который наследует от FirestoreAdapter и имплементируем методы onDataChanged() и onError()
        mRatingAdapter = new RatingAdapter(ratingsQuery) {
            @Override
            protected void onDataChanged() {
                if (getItemCount() == 0) {
                    mRatingsRecycler.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mRatingsRecycler.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            }
        };
        mRatingsRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRatingsRecycler.setAdapter(mRatingAdapter);

        // Создаем Rating Dialog
        mRatingDialog = new RatingDialogFragment();
    }

    // определяем действия на старте этой Activity
    @Override
    public void onStart() {
        super.onStart();
        // Устанавливаем слушатель на ресторан (нам возвращается регистрация/подписка, которую сохраняем в переменной mRestaurantRegistration)
        mRatingAdapter.startListening();
        mRestaurantRegistration = mRestaurantRef.addSnapshotListener(this);
    }

    // прекращаем слушать если Activity идет на уничтожение
    @Override
    public void onStop() {
        super.onStop();

        mRatingAdapter.stopListening();

        if (mRestaurantRegistration != null) {
            mRestaurantRegistration.remove();
            mRestaurantRegistration = null;
        }
    }

    // переписываем метод finish(), добавляя анимацию
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }

    /**
     * Слушатель для конкретного ресторана ({@link #mRestaurantRef}).
     */
    // когда приходит информация о ресторане сохраняем его
    @Override
    public void onEvent(DocumentSnapshot snapshot, FirebaseFirestoreException e) {
        if (e != null) {
            Log.w(TAG, "restaurant:onEvent", e);
            return;
        }

        onRestaurantLoaded(snapshot.toObject(Restaurant.class));
    }

    // Когда ресторан загружен, отображаем полученную информацию в UI
    private void onRestaurantLoaded(Restaurant restaurant) {
        mNameView.setText(restaurant.getName());
        mRatingIndicator.setRating((float) restaurant.getAvgRating());
        mNumRatingsView.setText(getString(R.string.fmt_num_ratings, restaurant.getNumRatings()));
        mCityView.setText(restaurant.getCity());
        mCategoryView.setText(restaurant.getCategory());
        mPriceView.setText(RestaurantUtil.getPriceString(restaurant));

        // Изображение на бэкграунде
        Glide.with(mImageView.getContext())
                .load(restaurant.getPhoto())
                .into(mImageView);
    }

    // Если нажата кнопка-крести вызываем метод для возврата назад в MainActivity
    @OnClick(R.id.restaurant_button_back)
    public void onBackArrowClicked(View view) {
        onBackPressed();
    }

    // Если нажата кнопка добавления рейтинга, отобрази диалоговое окно
    @OnClick(R.id.fab_show_rating_dialog)
    public void onAddRatingClicked(View view) {
        mRatingDialog.show(getSupportFragmentManager(), RatingDialogFragment.TAG);
    }

    // Метод, вызываемый в случае заполнения рейтинга пользователем и нажатия им на кнопку
    @Override
    public void onRating(Rating rating) {
        // Добавляем рейтинг и устанавливаем слушатель успеха/провала и выводим в лог результат
        addRating(mRestaurantRef, rating)
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Рейтинг добавлен");

                        // Hide keyboard and scroll to top
                        hideKeyboard();
                        mRatingsRecycler.smoothScrollToPosition(0);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Добавление рейтинга провалилось", e);

                        // Показываем сообщение пользователю, если добавление рейтинга провалилось
                        hideKeyboard();
                        Snackbar.make(findViewById(android.R.id.content), "Добавление рейтинга провалилось",
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    // Метод добавление рейтинга
    private Task<Void> addRating(final DocumentReference restaurantRef, final Rating rating) {
        // Создаем ссылку на новый рейтинг, для использования в транзакции
        // (транзакция - это операция, которая вносит все изменения или не вносит ни одного изменения)
        final DocumentReference ratingRef = restaurantRef.collection("ratings").document();

        // В транзакции, добавлением новый рейтинг и обновленяем общий рейтинг ресторана
        return mFirestore.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                // Берем ресторан
                Restaurant restaurant = transaction.get(restaurantRef).toObject(Restaurant.class);

                // Считаем количество рейтинга
                int newNumRatings = restaurant.getNumRatings() + 1;

                // Считаем общий рейтинг ресторана
                double oldRatingTotal = restaurant.getAvgRating() * restaurant.getNumRatings();
                double newAvgRating = (oldRatingTotal + rating.getRating()) / newNumRatings;

                // Устанавливаем новую информацию на ресторан
                restaurant.setNumRatings(newNumRatings);
                restaurant.setAvgRating(newAvgRating);

                // Загружаем все в Firestore
                transaction.set(restaurantRef, restaurant);
                transaction.set(ratingRef, rating);

                return null;
            }
        });
    }

    // скрываем клавиатуру
    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
