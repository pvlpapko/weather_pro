package com.ltm.weather;

import android.content.Context;

import java.util.Locale;

public final class L10n {
    public static final String LANG_RU = "ru";
    public static final String LANG_BE = "be";
    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";
    public static final String LANG_JA = "ja";
    public static final String LANG_KO = "ko";
    public static final String LANG_VI = "vi";

    private L10n() {}

    public static String lang(Context context) {
        return WeatherRepository.getLanguage(context);
    }

    public static String normalizeLanguage(String language) {
        if (language == null) return LANG_RU;
        String clean = language.trim().toLowerCase(Locale.US);
        if (clean.startsWith("be")) return LANG_BE;
        if (clean.startsWith("en")) return LANG_EN;
        if (clean.startsWith("zh")) return LANG_ZH;
        if (clean.startsWith("ja")) return LANG_JA;
        if (clean.startsWith("ko")) return LANG_KO;
        if (clean.startsWith("vi")) return LANG_VI;
        return LANG_RU;
    }

    public static Locale locale(Context context) {
        return locale(lang(context));
    }

    public static Locale locale(String language) {
        switch (normalizeLanguage(language)) {
            case LANG_BE: return new Locale("be");
            case LANG_EN: return Locale.ENGLISH;
            case LANG_ZH: return Locale.SIMPLIFIED_CHINESE;
            case LANG_JA: return Locale.JAPANESE;
            case LANG_KO: return Locale.KOREAN;
            case LANG_VI: return new Locale("vi");
            default: return new Locale("ru");
        }
    }

    public static String languageName(String language) {
        switch (normalizeLanguage(language)) {
            case LANG_BE: return "Беларуская";
            case LANG_EN: return "English";
            case LANG_ZH: return "中文";
            case LANG_JA: return "日本語";
            case LANG_KO: return "한국어";
            case LANG_VI: return "Tiếng Việt";
            default: return "Русский";
        }
    }

    public static String t(Context context, String key) {
        return t(lang(context), key);
    }

    public static String t(String language, String key) {
        String l = normalizeLanguage(language);
        switch (key) {
            case "app_title": return pick(l, "Weather Widget Pro", "Weather Widget Pro", "Weather Widget Pro", "Weather Widget Pro", "Weather Widget Pro", "Weather Widget Pro", "Weather Widget Pro");
            case "settings": return pick(l, "Настройки", "Налады", "Settings", "设置", "設定", "설정", "Cài đặt");
            case "back": return pick(l, "Назад", "Назад", "Back", "返回", "戻る", "뒤로", "Quay lại");
            case "language": return pick(l, "Язык", "Мова", "Language", "语言", "言語", "언어", "Ngôn ngữ");
            case "language_hint": return pick(l, "Нажми на текущий язык и выбери нужный в списке.", "Націсні на бягучую мову і выберы патрэбную ў спісе.", "Tap the current language and choose one from the list.", "点按当前语言并从列表中选择。", "現在の言語をタップしてリストから選びます。", "현재 언어를 눌러 목록에서 선택하세요.", "Chạm vào ngôn ngữ hiện tại và chọn trong danh sách.");
            case "places": return pick(l, "Города и места", "Гарады і месцы", "Cities and places", "城市和地点", "都市と場所", "도시와 장소", "Thành phố và địa điểm");
            case "current": return pick(l, "Текущий: ", "Бягучы: ", "Current: ", "当前：", "現在: ", "현재: ", "Hiện tại: ");
            case "enter_city": return pick(l, "Введите город", "Увядзіце горад", "Enter a city", "输入城市", "都市を入力", "도시 입력", "Nhập thành phố");
            case "find_select": return pick(l, "Найти и выбрать", "Знайсці і выбраць", "Find and select", "查找并选择", "検索して選択", "찾아서 선택", "Tìm và chọn");
            case "quick_pick": return pick(l, "Быстрый выбор", "Хуткі выбар", "Quick pick", "快速选择", "クイック選択", "빠른 선택", "Chọn nhanh");
            case "choose_from_list": return pick(l, "Выбрать из списка", "Выбраць са спіса", "Choose from list", "从列表选择", "リストから選択", "목록에서 선택", "Chọn từ danh sách");
            case "open_tray": return pick(l, "Открыть выбор", "Адкрыць выбар", "Open chooser", "打开选择", "選択を開く", "선택 열기", "Mở lựa chọn");
            case "clear_saved": return pick(l, "Очистить сохранённые", "Ачысціць захаваныя", "Clear saved", "清除已保存", "保存済みを消去", "저장 항목 지우기", "Xóa mục đã lưu");
            case "list_cleared": return pick(l, "Список очищен", "Спіс ачышчаны", "List cleared", "列表已清除", "リストを消去しました", "목록을 지웠습니다", "Đã xóa danh sách");
            case "searching_city": return pick(l, "Ищу город...", "Шукаю горад...", "Searching city...", "正在查找城市...", "都市を検索中...", "도시 검색 중...", "Đang tìm thành phố...");
            case "selected": return pick(l, "Выбрано: ", "Выбрана: ", "Selected: ", "已选择：", "選択: ", "선택됨: ", "Đã chọn: ");
            case "city_not_found": return pick(l, "Город не найден: ", "Горад не знойдзены: ", "City not found: ", "未找到城市：", "都市が見つかりません: ", "도시를 찾을 수 없음: ", "Không tìm thấy thành phố: ");
            case "app_theme": return pick(l, "Тема приложения", "Тэма праграмы", "App theme", "应用主题", "アプリのテーマ", "앱 테마", "Giao diện ứng dụng");
            case "light": return pick(l, "Светлая", "Светлая", "Light", "浅色", "ライト", "밝게", "Sáng");
            case "dark": return pick(l, "Тёмная", "Цёмная", "Dark", "深色", "ダーク", "어둡게", "Tối");
            case "follow_system": return pick(l, "Как в системе", "Як у сістэме", "Follow system", "跟随系统", "システムに合わせる", "시스템 설정", "Theo hệ thống");
            case "units": return pick(l, "Единицы измерения", "Адзінкі вымярэння", "Units", "单位", "単位", "단위", "Đơn vị");
            case "temperature": return pick(l, "Температура", "Тэмпература", "Temperature", "温度", "気温", "기온", "Nhiệt độ");
            case "wind": return pick(l, "Ветер", "Вецер", "Wind", "风", "風", "바람", "Gió");
            case "pressure": return pick(l, "Давление", "Ціск", "Pressure", "气压", "気圧", "기압", "Áp suất");
            case "widgets": return pick(l, "Виджеты", "Віджэты", "Widgets", "小组件", "ウィジェット", "위젯", "Tiện ích");
            case "lockscreen_weather": return pick(l, "Погода на экране блокировки / AOD", "Надвор’е на экране блакіроўкі / AOD", "Weather on lock screen / AOD", "锁屏/AOD 天气", "ロック画面/AOD の天気", "잠금 화면/AOD 날씨", "Thời tiết trên màn hình khóa / AOD");
            case "lockscreen_weather_hint": return pick(l, "Показывает тихое постоянное уведомление с погодой. На AOD видно там, где это разрешает оболочка телефона.", "Паказвае ціхае пастаяннае апавяшчэнне з надвор’ем. На AOD бачна там, дзе гэта дазваляе абалонка тэлефона.", "Shows a silent persistent weather notification. AOD display depends on the phone skin.", "显示静默常驻天气通知。AOD 是否显示取决于手机系统。", "静かな常時天気通知を表示します。AOD 表示は端末メーカーに依存します。", "조용한 상시 날씨 알림을 표시합니다. AOD 표시는 제조사 설정에 따라 다릅니다.", "Hiển thị thông báo thời tiết cố định và im lặng. AOD phụ thuộc vào giao diện điện thoại.");
            case "lockscreen_weather_on": return pick(l, "Погода на экране блокировки включена", "Надвор’е на экране блакіроўкі ўключана", "Lock screen weather enabled", "锁屏天气已开启", "ロック画面の天気をオン", "잠금 화면 날씨 켜짐", "Đã bật thời tiết màn hình khóa");
            case "lockscreen_weather_off": return pick(l, "Погода на экране блокировки выключена", "Надвор’е на экране блакіроўкі выключана", "Lock screen weather disabled", "锁屏天气已关闭", "ロック画面の天気をオフ", "잠금 화면 날씨 꺼짐", "Đã tắt thời tiết màn hình khóa");
            case "opacity": return pick(l, "Прозрачность: ", "Празрыстасць: ", "Transparency: ", "透明度：", "透明度: ", "투명도: ", "Độ trong suốt: ");
            case "info_mode": return pick(l, "Режим информации", "Рэжым інфармацыі", "Information mode", "信息模式", "情報モード", "정보 모드", "Chế độ thông tin");
            case "compact": return pick(l, "Компакт", "Кампактны", "Compact", "紧凑", "コンパクト", "간단히", "Gọn");
            case "normal": return pick(l, "Обычный", "Звычайны", "Normal", "普通", "標準", "보통", "Thường");
            case "detailed": return pick(l, "Подробный", "Падрабязны", "Detailed", "详细", "詳細", "자세히", "Chi tiết");
            case "light_text": return pick(l, "Светлый текст", "Светлы тэкст", "Light text", "浅色文字", "明るい文字", "밝은 글자", "Chữ sáng");
            case "dark_widget": return pick(l, "Тёмный вид", "Цёмны выгляд", "Dark widget", "深色小组件", "暗いウィジェット", "어두운 위젯", "Tiện ích tối");
            case "weather_refresh_interval": return pick(l, "Интервал обновления погоды", "Інтэрвал абнаўлення надвор'я", "Weather update interval", "天气更新间隔", "天気更新間隔", "날씨 업데이트 간격", "Khoảng cập nhật thời tiết");
            case "weather_refresh_hint": return pick(l, "Виджеты будут загружать свежие данные с выбранной частотой.", "Віджэты будуць загружаць свежыя даныя з выбранай частатой.", "Widgets will fetch fresh data at the selected interval.", "小组件会按所选间隔获取最新数据。", "ウィジェットは選択した間隔で新しいデータを取得します。", "위젯이 선택한 간격으로 새 데이터를 가져옵니다.", "Tiện ích sẽ tải dữ liệu mới theo khoảng đã chọn.");
            case "minutes_short": return pick(l, " мин", " хв", " min", " 分钟", "分", "분", " phút");
            case "hours_short": return pick(l, " ч", " г", " h", " 小时", "時間", "시간", " giờ");
            case "clock_size": return pick(l, "Размер часов: ", "Памер гадзінніка: ", "Clock size: ", "时钟大小：", "時計サイズ: ", "시계 크기: ", "Cỡ đồng hồ: ");
            case "weather_size": return pick(l, "Размер погоды: ", "Памер надвор’я: ", "Weather size: ", "天气大小：", "天気サイズ: ", "날씨 크기: ", "Cỡ thời tiết: ");
            case "rain_alerts": return pick(l, "Уведомления о дожде", "Апавяшчэнні пра дождж", "Rain alerts", "降雨提醒", "雨の通知", "비 알림", "Cảnh báo mưa");
            case "no_uv_night": return pick(l, "ночью нет", "уначы няма", "none at night", "夜间无", "夜間なし", "밤에는 없음", "không có ban đêm");
            case "enabled": return pick(l, "Включены", "Уключаны", "Enabled", "已开启", "オン", "켜짐", "Bật");
            case "disabled": return pick(l, "Выключены", "Выключаны", "Disabled", "已关闭", "オフ", "꺼짐", "Tắt");
            case "rain_alerts_enabled": return pick(l, "Уведомления о дожде включены", "Апавяшчэнні пра дождж уключаны", "Rain alerts enabled", "降雨提醒已开启", "雨の通知をオンにしました", "비 알림을 켰습니다", "Đã bật cảnh báo mưa");
            case "rain_alerts_disabled": return pick(l, "Уведомления выключены", "Апавяшчэнні выключаны", "Alerts disabled", "提醒已关闭", "通知をオフにしました", "알림을 껐습니다", "Đã tắt cảnh báo");
            case "rain_threshold": return pick(l, "Порог дождя: ", "Парог дажджу: ", "Rain threshold: ", "降雨阈值：", "雨のしきい値: ", "비 기준: ", "Ngưỡng mưa: ");
            case "widget_setup": return pick(l, "Настройка виджета", "Наладка віджэта", "Widget setup", "小组件设置", "ウィジェット設定", "위젯 설정", "Thiết lập tiện ích");
            case "widget_setup_hint": return pick(l, "Выбери вид, прозрачность и масштаб. Размер самого виджета меняется удержанием на рабочем столе.", "Выберы выгляд, празрыстасць і маштаб. Памер самога віджэта змяняецца ўтрыманнем на працоўным стале.", "Choose style, transparency, and scale. Resize the widget itself by long-pressing it on the home screen.", "选择外观、透明度和缩放。长按桌面小组件可调整实际大小。", "見た目、透明度、スケールを選びます。実際のサイズはホーム画面で長押しして変更します。", "스타일, 투명도, 배율을 선택하세요. 실제 위젯 크기는 홈 화면에서 길게 눌러 조절합니다.", "Chọn kiểu, độ trong suốt và tỉ lệ. Nhấn giữ tiện ích trên màn hình chính để đổi kích thước thật.");
            case "preview": return pick(l, "Предпросмотр", "Перадпрагляд", "Preview", "预览", "プレビュー", "미리보기", "Xem trước");
            case "alarm_hint": return pick(l, "будильник / таймер", "будзільнік / таймер", "alarm / timer", "闹钟 / 计时器", "アラーム / タイマー", "알람 / 타이머", "báo thức / hẹn giờ");
            case "transparency_hint": return pick(l, "100% — полностью прозрачный фон, 0% — плотная подложка.", "100% — цалкам празрысты фон, 0% — шчыльная падкладка.", "100% is fully transparent, 0% is solid.", "100% 为完全透明，0% 为不透明。", "100% は完全透明、0% は不透明です。", "100%는 완전 투명, 0%는 불투명입니다.", "100% là trong suốt hoàn toàn, 0% là nền đặc.");
            case "info_density": return pick(l, "Плотность информации", "Шчыльнасць інфармацыі", "Information density", "信息密度", "情報密度", "정보 밀도", "Mật độ thông tin");
            case "widget_color": return pick(l, "Цвет виджета", "Колер віджэта", "Widget color", "小组件颜色", "ウィジェットの色", "위젯 색상", "Màu tiện ích");
            case "done": return pick(l, "Готово", "Гатова", "Done", "完成", "完了", "완료", "Xong");
            case "city": return pick(l, "Город", "Горад", "City", "城市", "都市", "도시", "Thành phố");
            case "tap_for_forecast": return pick(l, "нажми для прогноза", "націсні для прагнозу", "tap for forecast", "点按查看预报", "タップして予報", "예보 보기", "chạm để xem dự báo");
            case "updated_prefix": return pick(l, "обн. ", "абн. ", "upd. ", "更新 ", "更新 ", "갱신 ", "cập nhật ");
            case "update_required": return pick(l, "обновление...", "абнаўленне...", "updating...", "正在更新...", "更新中...", "업데이트 중...", "đang cập nhật...");
            case "open_app_to_update": return pick(l, "Открой приложение для обновления", "Адкрый праграму для абнаўлення", "Open the app to update", "打开应用以更新", "アプリを開いて更新", "업데이트하려면 앱을 여세요", "Mở ứng dụng để cập nhật");
            case "map": return pick(l, "Карта", "Карта", "Map", "地图", "地図", "지도", "Bản đồ");
            case "app_subtitle": return pick(l, "чистая погода, виджеты, радар", "чыстае надвор'е, віджэты, радар", "clean weather, widgets, radar", "简洁天气、小组件、雷达", "天気、ウィジェット、レーダー", "날씨, 위젯, 레이더", "thời tiết, tiện ích, radar");
            case "loading": return pick(l, "Загрузка...", "Загрузка...", "Loading...", "正在加载...", "読み込み中...", "로딩 중...", "Đang tải...");
            case "tap_city_hint": return pick(l, "нажми на город для выбора места", "націсні на горад для выбару месца", "tap the city to choose a place", "点按城市选择位置", "都市をタップして場所を選択", "도시를 눌러 위치 선택", "chạm vào thành phố để chọn nơi");
            case "refresh": return pick(l, "Обновить", "Абнавіць", "Refresh", "刷新", "更新", "새로고침", "Cập nhật");
            case "current_weather": return pick(l, "Текущая погода", "Бягучае надвор'е", "Current weather", "当前天气", "現在の天気", "현재 날씨", "Thời tiết hiện tại");
            case "forecast_range": return pick(l, "Диапазон прогноза", "Дыяпазон прагнозу", "Forecast range", "预报范围", "予報範囲", "예보 범위", "Phạm vi dự báo");
            case "days_short": return pick(l, " дн.", " дз.", " d", " 天", "日", "일", " ngày");
            case "useful_indicators": return pick(l, "Полезные показатели", "Карысныя паказчыкі", "Useful indicators", "实用指标", "便利な指標", "유용한 지표", "Chỉ số hữu ích");
            case "hourly": return pick(l, "По часам", "Па гадзінах", "Hourly", "逐小时", "時間ごと", "시간별", "Theo giờ");
            case "forecast_for": return pick(l, "Прогноз на ", "Прагноз на ", "Forecast for ", "预报 ", "予報 ", "예보 ", "Dự báo ");
            case "days": return pick(l, " дней", " дзён", " days", " 天", "日間", "일", " ngày");
            case "precip_map": return pick(l, "Карта осадков", "Карта ападкаў", "Precipitation map", "降水地图", "降水マップ", "강수 지도", "Bản đồ mưa");
            case "open_live_radar": return pick(l, "Открыть живую радар-карту", "Адкрыць жывую радар-карту", "Open live radar map", "打开实时雷达地图", "ライブレーダーマップを開く", "실시간 레이더 지도 열기", "Mở bản đồ radar trực tiếp");
            case "footer": return pick(l, "Open-Meteo + RainViewer. Без рекламы и трекеров.", "Open-Meteo + RainViewer. Без рэкламы і трэкераў.", "Open-Meteo + RainViewer. No ads or trackers.", "Open-Meteo + RainViewer。无广告、无跟踪器。", "Open-Meteo + RainViewer。広告とトラッカーなし。", "Open-Meteo + RainViewer. 광고와 추적기 없음.", "Open-Meteo + RainViewer. Không quảng cáo, không theo dõi.");
            case "updated": return pick(l, "Обновлено: ", "Абноўлена: ", "Updated: ", "已更新：", "更新: ", "업데이트: ", "Đã cập nhật: ");
            case "updating": return pick(l, "Загрузка...", "Загрузка...", "Loading...", "正在加载...", "読み込み中...", "로딩 중...", "Đang tải...");
            case "failed_update": return pick(l, "Не удалось обновить: ", "Не ўдалося абнавіць: ", "Could not update: ", "无法更新：", "更新できません: ", "업데이트 실패: ", "Không cập nhật được: ");
            case "finding_location": return pick(l, "Определяю населённый пункт и улицу...", "Вызначаю населены пункт і вуліцу...", "Finding city and street...", "正在定位城市和街道...", "都市と通りを確認中...", "도시와 거리를 찾는 중...", "Đang xác định thành phố và đường...");
            case "location_failed": return pick(l, "Не удалось получить местоположение. Проверь GPS и разрешение геолокации.", "Не ўдалося атрымаць месцазнаходжанне. Правер GPS і дазвол геалакацыі.", "Could not get location. Check GPS and location permission.", "无法获取位置。请检查 GPS 和定位权限。", "位置を取得できません。GPS と位置情報権限を確認してください。", "위치를 가져올 수 없습니다. GPS와 위치 권한을 확인하세요.", "Không lấy được vị trí. Kiểm tra GPS và quyền vị trí.");
            case "location": return pick(l, "Местоположение: ", "Месцазнаходжанне: ", "Location: ", "位置：", "位置: ", "위치: ", "Vị trí: ");
            case "location_weather_failed": return pick(l, "Место получено, но погоду загрузить не удалось: ", "Месца атрымана, але надвор'е загрузіць не ўдалося: ", "Location found, but weather could not be loaded: ", "已获取位置，但天气加载失败：", "位置は取得しましたが天気を読み込めません: ", "위치는 찾았지만 날씨를 불러오지 못했습니다: ", "Đã có vị trí nhưng không tải được thời tiết: ");
            case "feels": return pick(l, "Ощущается", "Адчуваецца", "Feels", "体感", "体感", "체감", "Cảm giác");
            case "humidity": return pick(l, "Влажность", "Вільготнасць", "Humidity", "湿度", "湿度", "습도", "Độ ẩm");
            case "clouds": return pick(l, "Облачность", "Воблачнасць", "Clouds", "云量", "雲量", "구름", "Mây");
            case "precipitation": return pick(l, "Осадки", "Ападкі", "Precipitation", "降水", "降水", "강수", "Mưa");
            case "sunrise": return pick(l, "Восход", "Узыход", "Sunrise", "日出", "日の出", "일출", "Bình minh");
            case "sunset": return pick(l, "Закат", "Захад", "Sunset", "日落", "日の入り", "일몰", "Hoàng hôn");
            case "daylight": return pick(l, "День", "Дзень", "Daylight", "白昼", "日照", "낮", "Ban ngày");
            case "day_temp": return pick(l, "День", "Дзень", "Day", "白天", "昼", "낮", "Ngày");
            case "night_temp": return pick(l, "Ночь", "Ноч", "Night", "夜间", "夜", "밤", "Đêm");
            case "day_short": return pick(l, "Дн.", "Дз.", "Day", "昼", "昼", "낮", "Ngày");
            case "night_short": return pick(l, "Ноч.", "Ноч.", "Night", "夜", "夜", "밤", "Đêm");
            case "gusts": return pick(l, "Порывы", "Парывы", "Gusts", "阵风", "突風", "돌풍", "Gió giật");
            case "forecast": return pick(l, "Прогноз", "Прагноз", "Forecast", "预报", "予報", "예보", "Dự báo");
            case "selected_forecast": return pick(l, "Прогноз: ", "Прагноз: ", "Forecast: ", "预报：", "予報: ", "예보: ", "Dự báo: ");
            case "rain_map_toast": return pick(l, "Карта осадков: RainViewer + OpenStreetMap", "Карта ападкаў: RainViewer + OpenStreetMap", "Precipitation map: RainViewer + OpenStreetMap", "降水地图：RainViewer + OpenStreetMap", "降水マップ: RainViewer + OpenStreetMap", "강수 지도: RainViewer + OpenStreetMap", "Bản đồ mưa: RainViewer + OpenStreetMap");
            case "radar_title": return pick(l, "Weather Widget Pro · карта осадков", "Weather Widget Pro · карта ападкаў", "Weather Widget Pro · precipitation map", "Weather Widget Pro · 降水地图", "Weather Widget Pro · 降水マップ", "Weather Widget Pro · 강수 지도", "Weather Widget Pro · bản đồ mưa");
            case "radar_sub": return pick(l, "читаемая карта, свайп, масштаб и анимация осадков.", "радар RainViewer: свайп, пінч-маштаб і прагноз па часе.", "Readable map, pan, zoom, and precipitation animation.", "RainViewer 雷达：拖动、双指缩放和时间预报。", "RainViewer レーダー: 移動、ピンチズーム、時系列予測。", "RainViewer 레이더: 이동, 핀치 줌, 시간 예보.", "Radar RainViewer: kéo bản đồ, chụm phóng to và dự báo theo thời gian.");
            case "radar_loading": return pick(l, "Загружаю радар осадков...", "Загружаю радар ападкаў...", "Loading precipitation radar...", "正在加载降水雷达...", "降水レーダーを読み込み中...", "강수 레이더 로딩 중...", "Đang tải radar mưa...");
            case "radar_no_frames": return pick(l, "Радарные кадры не найдены", "Радарныя кадры не знойдзены", "No radar frames found", "未找到雷达帧", "レーダーフレームがありません", "레이더 프레임 없음", "Không có khung radar");
            case "radar_ready": return pick(l, "Радар обновлён", "Радар абноўлены", "Radar updated", "雷达已更新", "レーダー更新済み", "레이더 업데이트됨", "Radar đã cập nhật");
            case "radar_error": return pick(l, "Не удалось загрузить слой осадков", "Не ўдалося загрузіць слой ападкаў", "Could not load precipitation layer", "无法加载降水图层", "降水レイヤーを読み込めません", "강수 레이어를 불러오지 못했습니다", "Không tải được lớp mưa");
            case "rain_alert_channel": return pick(l, "Weather Widget Pro · дождь", "Weather Widget Pro · дождж", "Weather Widget Pro · rain", "Weather Widget Pro · 降雨", "Weather Widget Pro · 雨", "Weather Widget Pro · 비", "Weather Widget Pro · mưa");
            case "rain_alert_channel_desc": return pick(l, "Уведомления о вероятности дождя", "Апавяшчэнні пра верагоднасць дажджу", "Rain probability alerts", "降雨概率提醒", "雨の確率通知", "강수 확률 알림", "Cảnh báo khả năng mưa");
            case "rain_possible": return pick(l, "Возможен дождь: ", "Магчымы дождж: ", "Rain possible: ", "可能降雨：", "雨の可能性: ", "비 가능성: ", "Có thể mưa: ");
            case "probability": return pick(l, "вероятность", "верагоднасць", "probability", "概率", "確率", "확률", "khả năng");
            case "mm": return pick(l, "мм", "мм", "mm", "毫米", "mm", "mm", "mm");
            case "hour": return pick(l, "ч", "г", "h", "时", "時", "시", "giờ");
            case "min": return pick(l, "мин", "хв", "min", "分", "分", "분", "phút");
            case "rain": return pick(l, "дождь", "дождж", "rain", "降雨", "雨", "비", "mưa");
            default: return key;
        }
    }

    public static String intervalLabel(Context context, int minutes) {
        String l = lang(context);
        if (minutes < 60) return minutes + t(l, "minutes_short");
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (rest == 0) return hours + t(l, "hours_short");
        return hours + t(l, "hours_short") + " " + rest + t(l, "minutes_short");
    }

    public static String dailyLabel(String language, int index, String rawDate) {
        String l = normalizeLanguage(language);
        int weekday = weekdayIndex(rawDate);
        if (weekday >= 0) {
            switch (l) {
                case LANG_BE: return new String[]{"Пн", "Аў", "Ср", "Чц", "Пт", "Сб", "Нд"}[weekday];
                case LANG_EN: return new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}[weekday];
                case LANG_ZH: return new String[]{"周一", "周二", "周三", "周四", "周五", "周六", "周日"}[weekday];
                case LANG_JA: return new String[]{"月", "火", "水", "木", "金", "土", "日"}[weekday];
                case LANG_KO: return new String[]{"월", "화", "수", "목", "금", "토", "일"}[weekday];
                case LANG_VI: return new String[]{"T2", "T3", "T4", "T5", "T6", "T7", "CN"}[weekday];
                default: return new String[]{"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"}[weekday];
            }
        }
        if (rawDate != null && rawDate.length() >= 10) return rawDate.substring(8, 10) + "." + rawDate.substring(5, 7);
        int day = index + 1;
        switch (l) {
            case LANG_ZH: return "第" + day + "天";
            case LANG_JA: return day + "日目";
            case LANG_KO: return day + "일차";
            case LANG_VI: return "Ngày " + day;
            case LANG_EN: return "Day " + day;
            case LANG_BE: return "Дзень " + day;
            default: return "День " + day;
        }
    }

    private static int weekdayIndex(String rawDate) {
        if (rawDate == null || rawDate.length() < 10) return -1;
        try {
            int y = Integer.parseInt(rawDate.substring(0, 4));
            int m = Integer.parseInt(rawDate.substring(5, 7));
            int d = Integer.parseInt(rawDate.substring(8, 10));
            java.util.Calendar calendar = java.util.Calendar.getInstance(Locale.US);
            calendar.set(y, m - 1, d, 12, 0, 0);
            int day = calendar.get(java.util.Calendar.DAY_OF_WEEK);
            return day == java.util.Calendar.SUNDAY ? 6 : day - java.util.Calendar.MONDAY;
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static String weatherLabel(String language, int code) {
        String l = normalizeLanguage(language);
        switch (code) {
            case 0: return pick(l, "ясно", "ясна", "clear", "晴", "晴れ", "맑음", "trời quang");
            case 1: return pick(l, "почти ясно", "амаль ясна", "mostly clear", "大多晴朗", "ほぼ晴れ", "대체로 맑음", "gần như quang");
            case 2: return pick(l, "переменная облачность", "пераменная воблачнасць", "partly cloudy", "局部多云", "晴れ時々曇り", "구름 조금", "ít mây");
            case 3: return pick(l, "пасмурно", "пахмурна", "overcast", "阴", "曇り", "흐림", "nhiều mây");
            case 45:
            case 48: return pick(l, "туман", "туман", "fog", "雾", "霧", "안개", "sương mù");
            case 51:
            case 53:
            case 55: return pick(l, "морось", "імжа", "drizzle", "毛毛雨", "霧雨", "이슬비", "mưa phùn");
            case 56:
            case 57: return pick(l, "ледяная морось", "ледзяная імжа", "freezing drizzle", "冻毛毛雨", "凍る霧雨", "어는 이슬비", "mưa phùn đóng băng");
            case 61: return pick(l, "слабый дождь", "слабы дождж", "light rain", "小雨", "弱い雨", "약한 비", "mưa nhẹ");
            case 63: return pick(l, "дождь", "дождж", "rain", "雨", "雨", "비", "mưa");
            case 65: return pick(l, "сильный дождь", "моцны дождж", "heavy rain", "大雨", "強い雨", "강한 비", "mưa to");
            case 66:
            case 67: return pick(l, "ледяной дождь", "ледзяны дождж", "freezing rain", "冻雨", "凍雨", "어는 비", "mưa đóng băng");
            case 71: return pick(l, "слабый снег", "слабы снег", "light snow", "小雪", "弱い雪", "약한 눈", "tuyết nhẹ");
            case 73: return pick(l, "снег", "снег", "snow", "雪", "雪", "눈", "tuyết");
            case 75: return pick(l, "сильный снег", "моцны снег", "heavy snow", "大雪", "強い雪", "강한 눈", "tuyết dày");
            case 77: return pick(l, "снежная крупа", "снежная крупа", "snow grains", "雪粒", "雪あられ", "싸락눈", "tuyết hạt");
            case 80:
            case 81:
            case 82: return pick(l, "ливни", "ліўні", "showers", "阵雨", "にわか雨", "소나기", "mưa rào");
            case 85:
            case 86: return pick(l, "снегопад", "снегапад", "snow showers", "阵雪", "にわか雪", "눈 소나기", "mưa tuyết");
            case 95: return pick(l, "гроза", "навальніца", "thunderstorm", "雷暴", "雷雨", "뇌우", "dông");
            case 96:
            case 99: return pick(l, "гроза с градом", "навальніца з градам", "thunderstorm with hail", "雷暴伴冰雹", "ひょうを伴う雷雨", "우박 동반 뇌우", "dông kèm mưa đá");
            default: return pick(l, "погода", "надвор'е", "weather", "天气", "天気", "날씨", "thời tiết");
        }
    }

    private static String pick(String language, String ru, String be, String en, String zh, String ja, String ko, String vi) {
        switch (normalizeLanguage(language)) {
            case LANG_BE: return be;
            case LANG_EN: return en;
            case LANG_ZH: return zh;
            case LANG_JA: return ja;
            case LANG_KO: return ko;
            case LANG_VI: return vi;
            default: return ru;
        }
    }
}
