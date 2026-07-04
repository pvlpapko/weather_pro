# Подпись и обновление поверх установленной версии

`applicationId` текущего приложения:

`com.ltm.weather`

Текущая версия:

- `versionCode 25`
- `versionName 2.3.20`

## Главное правило

Чтобы APK/AAB устанавливались поверх уже установленного приложения как обновление, должны совпадать:

1. `applicationId`;
2. подпись приложения;
3. новый `versionCode` должен быть больше старого.

## Постоянная release-подпись

Для release-сборок нужно использовать **одну постоянную подпись**.

Нельзя каждый раз создавать новый keystore. Нельзя подписывать release-сборки случайным debug-ключом GitHub Actions. Если подпись меняется, Android покажет конфликт пакетов и не даст установить APK поверх старой версии.

В этом проекте debug и release подписываются через:

`signingConfigs.updateCompatible`

Если GitHub Secrets не заданы, используется стабильный тестовый ключ:

`app/dev-update-key.jks`

Для публикации в магазине лучше один раз создать свой постоянный release/upload key, добавить его в GitHub Secrets и больше не менять.

## GitHub Secrets для постоянной подписи

Поддерживаются новые имена:

- `WEATHER_KEYSTORE_BASE64`
- `WEATHER_KEYSTORE_PASSWORD`
- `WEATHER_KEY_ALIAS`
- `WEATHER_KEY_PASSWORD`

Также поддерживаются старые имена для совместимости:

- `LTM_KEYSTORE_BASE64`
- `LTM_KEYSTORE_PASSWORD`
- `LTM_KEY_ALIAS`
- `LTM_KEY_PASSWORD`

## Если уже есть конфликт пакетов

Если на телефоне уже установлена версия с тем же `applicationId`, но другой подписью, обновить её поверх невозможно без старого ключа подписи.

В таком случае нужно один раз удалить старую версию, установить сборку с текущей постоянной подписью, и дальше новые сборки будут устанавливаться поверх при сохранении этого же ключа и увеличении `versionCode`.

## Что нельзя менять между обновлениями

- `applicationId`;
- release-keystore/upload key;
- key alias;
- key password/store password без необходимости;
- схему подписи в `build.gradle`.

## Что нужно делать при каждой новой версии

- повышать `versionCode`;
- обновлять `versionName`;
- сохранять тот же ключ подписи;
- обновлять README, privacy-policy, fastlane metadata и changelog так, чтобы они описывали только реальные функции приложения.

## Как работает GitHub Actions

Если заданы `WEATHER_KEYSTORE_BASE64` или `LTM_KEYSTORE_BASE64`, workflow декодирует keystore во временный файл и передаёт путь в Gradle через переменные окружения `WEATHER_KEYSTORE_PATH` или `LTM_KEYSTORE_PATH`.

Если Secrets не заданы, workflow не создаёт новый случайный ключ, а использует стабильный `app/dev-update-key.jks` из проекта.
