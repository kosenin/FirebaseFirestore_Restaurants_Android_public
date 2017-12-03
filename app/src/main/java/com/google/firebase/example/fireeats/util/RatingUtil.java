package com.google.firebase.example.fireeats.util;

import com.google.firebase.example.fireeats.model.Rating;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Класс с общими методами и переменными и начальными фейковыми данными для рейтингов
 */
public class RatingUtil {

    public static final String[] REVIEW_CONTENTS = {
            // 0 - 1 звезды
            "Это было ужасно! Вообще несъедобно!",

            // 1 - 2 звезды
            "Это было довольно плохо, больше не пойду",

            // 2 - 3 звезды
            "Накормили, уже что-то",

            // 3 - 4 звезды
            "Ужин был хорошом, я бы вернулся",

            // 4 - 5 звезды
            "Просто потрясающе!  Лучше всех!"
    };

    /**
     * Берем лист случайных рейтингов
     */
    public static List<Rating> getRandomList(int length) {
        List<Rating> result = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            result.add(getRandom());
        }

        return result;
    }

    /**
     * Берем общий рейтинг в листе
     */
    public static double getAverageRating(List<Rating> ratings) {
        double sum = 0.0;

        for (Rating rating : ratings) {
            sum += rating.getRating();
        }

        return sum / ratings.size();
    }

    /**
     * Создаем случайных рейтинг
     */
    public static Rating getRandom() {
        Rating rating = new Rating();

        Random random = new Random();

        double score = random.nextDouble() * 5.0;
        String text = REVIEW_CONTENTS[(int) Math.floor(score)];

        rating.setUserId(UUID.randomUUID().toString());
        rating.setUserName("Случайный пользователь");
        rating.setRating(score);
        rating.setText(text);

        return rating;
    }

}
