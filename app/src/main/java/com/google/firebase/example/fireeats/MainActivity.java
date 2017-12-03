package com.google.firebase.example.fireeats;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.example.fireeats.adapter.RestaurantAdapter;
import com.google.firebase.example.fireeats.model.Rating;
import com.google.firebase.example.fireeats.model.Restaurant;
import com.google.firebase.example.fireeats.util.RatingUtil;
import com.google.firebase.example.fireeats.util.RestaurantUtil;
import com.google.firebase.example.fireeats.viewmodel.MainActivityViewModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;



// ЗАМЕТКА: Приложение использует Firestore, при использовании приложения в лог будут выводится ссылки для создания индексов в Консоли Firebase
// Посмотреть логкат (ctrl + 6 (Windows)/cmd + 6 (Mac)). Вводим ссылку в браузере и нас перенест в Firebase Консоль на страницу создания индекса

/**
 * Главная Activity
 */
public class MainActivity extends AppCompatActivity implements
        FilterDialogFragment.FilterListener,
        RestaurantAdapter.OnRestaurantSelectedListener {

    // константы
    private static final String TAG = "MainActivity";
    // переменная для аутентификации
    private static final int RC_SIGN_IN = 9001;
    private static final int LIMIT = 50;

    // связываем переменные с UI
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.text_current_search)
    TextView mCurrentSearchView;

    @BindView(R.id.text_current_sort_by)
    TextView mCurrentSortByView;

    @BindView(R.id.recycler_restaurants)
    RecyclerView mRestaurantsRecycler;

    @BindView(R.id.view_empty)
    ViewGroup mEmptyView;

    // переменные Firebase
    private FirebaseFirestore mFirestore;
    private Query mQuery;

    // диалог и адаптер
    private FilterDialogFragment mFilterDialog;
    private RestaurantAdapter mAdapter;

    // архитектурный компонент ViewModel
    private MainActivityViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Инициализируем UI
        super.onCreate(savedInstanceState);
        // "Надуваем" разметку
        setContentView(R.layout.activity_main);
        // запускаем Butterknife (библиотека для связывания UI с переменными)
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        // Инициализируем ViewModel
        mViewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        // Включаем глобальный логгинг для Firestore для SDK
        FirebaseFirestore.setLoggingEnabled(true);

        // Инициализируем Firestore
        mFirestore = FirebaseFirestore.getInstance();

        // Запрос по дефолту. Берем все рестораны и отображаем в нисходящем по рейтингу порядке
        mQuery = mFirestore.collection("restaurants")
                .orderBy("avgRating", Query.Direction.DESCENDING)
                .limit(LIMIT);

        // RecyclerView
        // создаем RestaurantAdapter, который наследует от FirestoreAdapter и имплементируем методы onDataChanged() и onError()
        mAdapter = new RestaurantAdapter(mQuery, this) {
            @Override
            protected void onDataChanged() {
                // Покажи/спрячь данные в UI если запрос возвращается пустым
                if (getItemCount() == 0) {
                    mRestaurantsRecycler.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mRestaurantsRecycler.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {
                // Покажи снакбар в случаи ошибки
                Snackbar.make(findViewById(android.R.id.content),
                        "Ошибка: смотрите логи", Snackbar.LENGTH_LONG).show();
            }
        };

        mRestaurantsRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRestaurantsRecycler.setAdapter(mAdapter);

        // Создаем Filter Dialog
        mFilterDialog = new FilterDialogFragment();
    }

    // определяем действия на старте этой Activity
    @Override
    public void onStart() {
        super.onStart();

        // Начинаем процесс логина если требуется
        if (shouldStartSignIn()) {
            startSignIn();
            return;
        }

        // Применяем фильтры
        onFilter(mViewModel.getFilters());

        // Начинаем слушать апдейты Firestore
        if (mAdapter != null) {
            mAdapter.startListening();
        }
    }

    // убираем слушатель если Activity идет на уничтожение
    @Override
    public void onStop() {
        super.onStop();
        if (mAdapter != null) {
            mAdapter.stopListening();
        }
    }

    // меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_items:
                onAddItemsClicked();
                break;
            case R.id.menu_sign_out:
                AuthUI.getInstance().signOut(this);
                startSignIn();
                break;
            case R.id.menu_delete:
                RestaurantUtil.deleteAll();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // вызывается после выхода с процесса логина
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            mViewModel.setIsSigningIn(false);

            if (resultCode != RESULT_OK && shouldStartSignIn()) {
                startSignIn();
            }
        }
    }

    // слушатель на view поиска
    @OnClick(R.id.filter_bar)
    public void onFilterClicked() {
        // Показать диалог содержащий фильтр для поиска ресторанов
        mFilterDialog.show(getSupportFragmentManager(), FilterDialogFragment.TAG);
    }

    // слушатель на кнопку крестика, удалем фильтры
    @OnClick(R.id.button_clear_filter)
    public void onClearFilterClicked() {
        mFilterDialog.resetFilters();
        onFilter(Filters.getDefault());
    }

    // колбак от адаптера (выбран ресторан)
    @Override
    public void onRestaurantSelected(DocumentSnapshot restaurant) {
        // Идем на детальную страницу для выбранного ресторана
        Intent intent = new Intent(this, RestaurantDetailActivity.class);
        intent.putExtra(RestaurantDetailActivity.KEY_RESTAURANT_ID, restaurant.getId());
        startActivity(intent);
        // анимация перехода
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
    }

    // коллбак от диалога с фильтрами (выбор ресторана по параметрам)
    @Override
    public void onFilter(Filters filters) {
        // Создаем базисный запрос
        Query query = mFirestore.collection("restaurants");

        // Берем выбранную категорию и добавляем к запросу (конкретизируем запрос)
        if (filters.hasCategory()) {
            query = query.whereEqualTo(Restaurant.FIELD_CATEGORY, filters.getCategory());
        }

        // Берем выбранный город и добавляем к запросу (конкретизируем запрос)
        if (filters.hasCity()) {
            query = query.whereEqualTo(Restaurant.FIELD_CITY, filters.getCity());
        }

        // Берем выбранную цену и добавляем к запросу (конкретизируем запрос)
        if (filters.hasPrice()) {
            query = query.whereEqualTo(Restaurant.FIELD_PRICE, filters.getPrice());
        }

        // Берем выбранную сортировку и добавляем к запросу (конкретизируем запрос)
        if (filters.hasSortBy()) {
            query = query.orderBy(filters.getSortBy(), filters.getSortDirection());
        }

        // Ограничиваем запрос
        query = query.limit(LIMIT);

        // Обновляем запрос в адаптере
        mAdapter.setQuery(query);

        // Устаналиваем хедер
        mCurrentSearchView.setText(Html.fromHtml(filters.getSearchDescription(this)));
        mCurrentSortByView.setText(filters.getOrderDescription(this));

        // Сохраняем фильтер
        mViewModel.setFilters(filters);
    }

    // определяем, нужно ли логиниться
    private boolean shouldStartSignIn() {
        return (!mViewModel.getIsSigningIn() && FirebaseAuth.getInstance().getCurrentUser() == null);
    }

    // начинаем логин
    private void startSignIn() {
        // Логинимся с помощью FirebaseUI
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(Collections.singletonList(
                        new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()))
                .setIsSmartLockEnabled(false)
                .build();

        startActivityForResult(intent, RC_SIGN_IN);
        mViewModel.setIsSigningIn(true);
    }

    private void onAddItemsClicked() {
        // Добавляем несколько рандомных ресторанов в Firestore
        WriteBatch batch = mFirestore.batch();
        for (int i = 0; i < 10; i++) {
            DocumentReference restRef = mFirestore.collection("restaurants").document();

            // Создаем рандомные рестораны/рейтинги
            Restaurant randomRestaurant = RestaurantUtil.getRandom(this);
            List<Rating> randomRatings = RatingUtil.getRandomList(randomRestaurant.getNumRatings());
            randomRestaurant.setAvgRating(RatingUtil.getAverageRating(randomRatings));

            // Добавляем ресторан
            batch.set(restRef, randomRestaurant);

            // Добавляем рейтинг в ресторан
            for (Rating rating : randomRatings) {
                batch.set(restRef.collection("ratings").document(), rating);
            }
        }
        // атомарно добавляем все в Firestore и устанавливаем слушатель успеха
        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Запись прошла успешно");
                } else {
                    Log.w(TAG, "Запись провалилась", task.getException());
                }
            }
        });
    }
}
