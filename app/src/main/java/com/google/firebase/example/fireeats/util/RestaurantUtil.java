package com.google.firebase.example.fireeats.util;

import android.content.Context;
import android.support.annotation.WorkerThread;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.example.fireeats.R;
import com.google.firebase.example.fireeats.model.Restaurant;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Класс с общими методами и переменными и начальными фейковыми данными для ресторанов
 */
public class RestaurantUtil {

    private static final String TAG = "RestaurantUtil";

    // Executor для работы с потоками (thread)
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(2, 4, 60,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    // ссылка на случайные изображения
    private static final String RESTAURANT_URL_FMT = "https://storage.googleapis.com/firestorequickstarts.appspot.com/food_%d.png";
    private static final int MAX_IMAGE_NUM = 22;

    // Фейковые данные ресторанов
    private static final String[] NAME_FIRST_WORDS = {
            "Foo",
            "Bar",
            "Baz",
            "Qux",
            "Fire",
            "Sam's",
            "World Famous",
            "Google",
            "The Best",
    };

    private static final String[] NAME_SECOND_WORDS = {
            "Restaurant",
            "Cafe",
            "Spot",
            "Eatin' Place",
            "Eatery",
            "Drive Thru",
            "Diner",
    };


    /**
     * Создаем случайных ресторан
     */
    public static Restaurant getRandom(Context context) {
        Restaurant restaurant = new Restaurant();
        Random random = new Random();

        // Города
        String[] cities = context.getResources().getStringArray(R.array.cities);
        cities = Arrays.copyOfRange(cities, 1, cities.length);

        // Категории
        String[] categories = context.getResources().getStringArray(R.array.categories);
        categories = Arrays.copyOfRange(categories, 1, categories.length);

        int[] prices = new int[]{1, 2, 3};

        restaurant.setName(getRandomName(random));
        restaurant.setCity(getRandomString(cities, random));
        restaurant.setCategory(getRandomString(categories, random));
        restaurant.setPhoto(getRandomImageUrl(random));
        restaurant.setPrice(getRandomInt(prices, random));
        restaurant.setNumRatings(random.nextInt(20));

        // Общий рейтинг не создан специально
        return restaurant;
    }


    /**
     * Берем случайное изображение
     */
    private static String getRandomImageUrl(Random random) {
        // Integer between 1 and MAX_IMAGE_NUM (inclusive)
        int id = random.nextInt(MAX_IMAGE_NUM) + 1;

        return String.format(Locale.getDefault(), RESTAURANT_URL_FMT, id);
    }

    /**
     * Берем цену в виде ввиде знака доллара
     */
    public static String getPriceString(Restaurant restaurant) {
        return getPriceString(restaurant.getPrice());
    }

    /**
     * Берем цену в виде ввиде знака доллара (помогающий метод)
     */
    public static String getPriceString(int priceInt) {
        switch (priceInt) {
            case 1:
                return "$";
            case 2:
                return "$$";
            case 3:
            default:
                return "$$$";
        }
    }

    /**
     * Удаляем все документы в коллекции. Используем Executor для выполнения работы в бэкграунде (в отдельном потоке)
     * Это не находит и не удаляет автоматически все субколлекции
     */
    private static Task<Void> deleteCollection(final CollectionReference collection,
                                               final int batchSize,
                                               Executor executor) {

        // Выполняем операцию удаления с помощью Executor, который позволяет нам использовать
        // простую логику без блокировки главного(UI) потока (Main thread/UI thread)
        return Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Берем первую группу документов в коллекции
                Query query = collection.orderBy("__name__").limit(batchSize);

                // Берем лист удаленных документов
                List<DocumentSnapshot> deleted = deleteQueryBatch(query);

                // В то время как удаленные документы в последней группе показывают что могут
                // быть другие документы в коллекции, возьми следующую группу и удали снова
                while (deleted.size() >= batchSize) {
                    // Передвинь курсор запроса для начала  после последнего документа в группе
                    DocumentSnapshot last = deleted.get(deleted.size() - 1);
                    query = collection.orderBy("__name__")
                            .startAfter(last.getId())
                            .limit(batchSize);

                    deleted = deleteQueryBatch(query);
                }

                return null;
            }
        });

    }

    /**
     * Удаляет все результаты из запроса в отдельной операции записи. Должна быть проведена на рабочем потоке (worker thread)
     * для избежания блокирования/крэша на главном потоке (main thread)
     */
    @WorkerThread
    private static List<DocumentSnapshot> deleteQueryBatch(final Query query) throws Exception {
        QuerySnapshot querySnapshot = Tasks.await(query.get());

        WriteBatch batch = query.getFirestore().batch();
        for (DocumentSnapshot snapshot : querySnapshot) {
            batch.delete(snapshot.getReference());
        }
        Tasks.await(batch.commit());

        return querySnapshot.getDocuments();
    }

    /**
     * Удаляет все рестораны
     */
    public static Task<Void> deleteAll() {
        CollectionReference ref = FirebaseFirestore.getInstance().collection("restaurants");
        return deleteCollection(ref, 25, EXECUTOR);
    }

    // Берем случайных рейтинг
    private static double getRandomRating(Random random) {
        double min = 1.0;
        return min + (random.nextDouble() * 4.0);
    }

    // Берем случайное имя для ресторана
    private static String getRandomName(Random random) {
        return getRandomString(NAME_FIRST_WORDS, random) + " "
                + getRandomString(NAME_SECOND_WORDS, random);
    }

    // Берем случайных String
    private static String getRandomString(String[] array, Random random) {
        int ind = random.nextInt(array.length);
        return array[ind];
    }

    // Берем случайное число
    private static int getRandomInt(int[] array, Random random) {
        int ind = random.nextInt(array.length);
        return array[ind];
    }

}
