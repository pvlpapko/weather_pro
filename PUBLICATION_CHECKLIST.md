# Подготовка к публикации

## Версия

- `applicationId`: `com.ltm.weather`
- `versionCode`: `26`
- `versionName`: `2.3.21`

## Что готово

- Release APK и Release AAB собираются через GitHub Actions workflow `Build Weather Widget Pro Android`.
- Release-сборка подписывается через `signingConfigs.updateCompatible`.
- Если заданы GitHub Secrets с постоянным ключом, используется он.
- Если Secrets не заданы, используется стабильный тестовый ключ `app/dev-update-key.jks`, чтобы сборки из этого проекта обновлялись поверх друг друга.
- README обновлён.
- Политика конфиденциальности обновлена.
- fastlane metadata обновлены.
- fastlane changelog для `versionCode 26` добавлен.
- Общий `CHANGELOG.md` добавлен.

## Перед публикацией в магазин

1. Использовать один постоянный release/upload key.
2. Добавить ключ в GitHub Secrets:
   - `WEATHER_KEYSTORE_BASE64`
   - `WEATHER_KEYSTORE_PASSWORD`
   - `WEATHER_KEY_ALIAS`
   - `WEATHER_KEY_PASSWORD`
3. Запустить GitHub Actions.
4. Для публикации использовать `app-release.aab` из артефакта `Weather-Widget-Pro-builds`.
5. Не менять `applicationId` и подпись между версиями.
