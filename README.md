# Firestore

## Введение

Это приложение, реализованное с помощью Firestore, показывает рестораны и их рейтинги с возможностью добавления рейтингов и обширной фильтрации выбора.

## Комментируемый код
Каждый блок кода подробно откоментирован на русском языке. Первый класс для чтения - MainActivity.

## Приложение

<img src="./docs/home.png" height="534" width="300"/>

## Как начать

* Скачайте данный проект
* Откройте его с помощью Андроид Студио
* В [Консоли Firebase][firebase-console] создайте новое приложение и включите аутентификацию по email ([как это сделать смотрим тут в разделе Подключение][sample-1])
* В Консоли Firebase в разделе Database включитe Firestore
* Подключите Firebase к вашему андроид проекту ([подробней тут][sample-2])
* В Консоли Firebase в разделе Database на вкладке Правила (Rules) установите следующие Правила безопасности:

```
service cloud.firestore {
  match /databases/{database}/documents {
    // Любой пользователь может читать данные ресторана, если он аутентифицирован
    // Пользователи могут создавать, обновлять или удалять их
  	 match /restaurants/{restaurantId} {
    	 allow read: if true;
    	 allow create, update, delete: if request.auth.uid != null;
    }

    // Любой пользователь может добавить рейтинг. Только пользователь
    // который создал рейтинг может его удалить. Рейтинги не могут быть обновлены.
    match /restaurants/{restaurantId}/ratings/{ratingId} {
    	 allow read: if true;
      allow create: if request.auth.uid != null;
    	 allow delete: if request.resource.data.userId == request.auth.uid;
    	 allow update: if false;
    }
  }
}
```
* Запустите приложение. Сначала пройдите процедуру регистрации и укажите email. Далее в самом приложении нажмите на Add Random Items чтобы добавить рестораны в лист.
PS: В случае проблем в build.gradle файле обновите библиотеки Firebase на более новые версии. Андроид Студио выделит их цветом и вы можете просто нажать Alt+Enter либо сделайте это вручную. Последнии версии библиотек Firebase доступны [здесь][firebase-libraries1] и [здесь][firebase-libraries2]

## Создание индексов
При первых использованиях функционала фильтрации результатов (например, фильтровать рестораны по рейтингу и по цене) вы логах (например в Logcat в Фндроид студио) вы сможете увидеть предупреждения вида:
```
com.google.firebase.example.fireeats W/Firestore Adapter: onEvent:error
com.google.firebase.firestore.FirebaseFirestoreException: FAILED_PRECONDITION: The query requires an index. You can create it here: https://console.firebase.google.com/project/...
```

Это происходит потому, что индексирование необходимо для создания запросов в Firestore. Нажмите на ссылку в этих предупреждениях и вы будете переведены в Консоль Firebase для создания индекса уже с заполненными для вас параметрами:

<img src="./docs/index.png" />

Это приложение также предоставляет файл спецификации индекса в `indexes.json`
который определяет все индексы, необходимые для запуска приложения. Ты можешь
добавить все эти индексы через терминал. Для этого введи команду firebase init firestore для создания JSON файла и firebase deploy --only firestore:indexes для загрузки индексов.

## Документация
Документация, новости и туториалы на русском языке доступны на сайте [firebase-info.com][firebaseinfo].

## Подпишись! :)
* Сайт: [firebase-info.com][firebaseinfo]
* VK: https://vk.com/firebaseinfo
* Телеграм: https://t.me/firebaseinfo
* Youtube: https://www.youtube.com/channel/UCkWmMVE_80TdTGj6fDuKORA
* Facebook: https://www.facebook.com/firebaseinfo/


[firebase-console]: console.firebase.google.com
[sample-1]: https://firebase-info.com/2017/06/10/%D0%BE%D1%81%D0%BD%D0%BE%D0%B2%D1%8B-firebase-3-3-firebase-%D0%B0%D1%83%D1%82%D0%B5%D0%BD%D1%82%D0%B8%D1%84%D0%B8%D0%BA%D0%B0%D1%86%D0%B8%D1%8F-%D0%BF%D0%BE%D0%B4%D0%BA%D0%BB%D1%8E%D1%87%D0%B5/
[sample-2]: https://www.youtube.com/watch?v=bOmkJdEtpjo
[firebase-libraries1]: https://firebase.google.com/support/release-notes/android
[firebase-libraries2]: https://firebase.google.com/docs/android/setup
[firebaseinfo]: https://firebase-info.com/%D0%BA%D0%B0%D1%80%D1%82%D0%B0-%D1%81%D0%B0%D0%B9%D1%82%D0%B0/