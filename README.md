<div align="center">

<img src="src/main/resources/assets/reroll_plus_plus/icon.png" width="128" alt="Reroll Plus Plus icon">

# Reroll Plus Plus

**Кнопки реролла сделок жителей и зачарований для Minecraft 26.3 (Fabric)**

![Minecraft](https://img.shields.io/badge/Minecraft-26.3-62a355)
![Fabric](https://img.shields.io/badge/Fabric-0.20.1-d3b06e)
![Version](https://img.shields.io/badge/version-2.0--beta-orange)
![License](https://img.shields.io/badge/license-CC0--1.0-lightgrey)

</div>

## Зачем этот мод

В ванильном Minecraft сделки жителя генерируются один раз при его прокачке, а зачарования в столе зачарований «крутятся» только при смене предмета или уровня. Чтобы получить «удачные» сделки (дешёвые ноты защищающего, максимальную скидку от бронника) или нужное зачарование, игроки ломают и расставляют стойки, убивают жителей, перекладывают предметы — это муторно.

**Reroll Plus Plus** добавляет кнопку с иконкой ↻ рядом с экраном торговли и стола зачарований: одно нажатие — и житель получает свежий полный набор сделок, а стол зачарований — три новых предложения. Не бесплатно: за реролл платим изумрудами или лазуритом, а торговец может и отказаться.

## Возможности

- Кнопка-вкладка ↻ 20×20 снаружи слева от экрана торговли: житель получает **свежий полный набор сделок** всех уровней 1..N через ванильные наборы (`TradeSet`) — житель 4-го уровня получает весь свой набор, а не только сделки верхнего уровня.
- Кнопка ↻ на столе зачарований: обновляет три предложения за 1 лазурит из слота стола.
- Поддержка **странствующих торговцев** (реролл за 1 изумруд).
- **Механика отказа**: торговец может не согласиться на реролл — шанс растёт с уровнем; при отказе изумруды не списываются, звучит `villager.no` и появляются злые частицы.
- **Горячая клавиша R** (переназначается в управлении) — реролл прямо из открытого экрана торговли.
- Цена отображается на кнопке в стиле счётчика предметов; при нехватке изумрудов кнопка неактивна.
- Звук и частицы при успехе (`villager.yes` + счастливые частицы) и при отказе.
- Кнопки и весь серверный функционал активны только когда мод стоит **с обеих сторон** — на ванильном сервере интерфейс не меняется.
- Локализация: английская и русская.

## Стоимость реролла

| Уровень сделок жителя | Цена | Шанс отказа |
|:---|---:|---:|
| Обычный (Novice) | 1 💚 | 0% |
| Подмастерье (Apprentice) | 2 💚 | 5% |
| Ремесленник (Journeyman) | 4 💚 | 10% |
| Эксперт (Expert) | 8 💚 | 15% |
| Мастер (Master) | 16 💚 | 20% |
| Странствующий торговец | 1 💚 | 10% |

- Реролл зачарований стоит **1 лазурит** из слота стола зачарований.
- Цены заданы в `RerollCost.EMERALDS_PER_LEVEL`, шансы отказа — в `RerollOdds` (потолок 30%); используются и клиентом (подсказка и блокировка кнопки), и сервером (списание). Изумруды и лазурит списываются только на серверной стороне; у жителей без профессий (нитвитов) оплата не взимается.

## Как это работает

Мод разделён на клиентскую и серверную часть (split source sets) и общается одним кастомным пакетом.

**Клиент**

- `MerchantScreenMixin` / `EnchantmentScreenMixin` добавляют `RerollButton` / `EnchantRerollButton` через ванильный `addRenderableWidget()`; доступ к координатам экрана — через accessor-миксины (`AbstractContainerScreenAccessor`, `AbstractContainerScreenMixin`), потому что в 26.3 у Minecraft нет обфускации и mixin refmap, а `@Shadow` унаследованных полей невозможен.
- Каждый кадр кнопки пересчитывают состояние: цену по уровню торговца, наличие изумрудов/лазурита и кадр текстуры (обычный / наведение / недоступный).
- Нажатие на экране торговли отправляет payload `reroll_plus_plus:reroll_trades` — сервер сам знает, с каким торговцем открыт экран, поэтому подменить цель нельзя.
- Реролл зачарований использует штатный механизм кнопок контейнера (`ServerboundContainerButtonClickPacket`, id 3) — отдельный пакет не нужен.
- Клавиша **R** регистрируется в `RerollTradesPlusClient` и обрабатывается `MerchantScreenMixin`, пока открыт экран торговли.

**Сервер**

Обработчик `ServerPlayNetworking` выполняется в потоке сервера и:

1. достаёт торговца из открытого у игрока `MerchantMenu` и проверяет валидность (`stillValid` — дистанция, живучесть; наблюдателю отказано);
2. бросает «кубик» на отказ (`RerollOdds`); при отказе — звук, злые частицы и выход без списания;
3. списывает изумруды (`RerollCost#tryCharge`); при нехватке — сообщение и выход;
4. очищает сделки и через invoker-миксины повторяет **ванильную** генерацию: `AbstractVillager#addOffersFromTradeSet` для каждого уровня 1..N профессии жителя (`WanderingTraderInvoker` делает то же для странствующего торговца);
5. `Villager#updateSpecialPrices` пересчитывает скидки (репутация, «Герой деревни») и рассылает новые предложения клиенту через `sendMerchantOffers`.

Реролл зачарований обрабатывается в `EnchantmentMenuMixin` внутри ванильного `clickMenuButton`: проверяется валидность, в слоте должен лежать зачаровываемый предмет и лазурит, затем обновляется сид зачарований и вызывается `slotsChanged` — новые предложения полностью ванильные и синхронизируются штатным `broadcastChanges`.

Никакие данные о сделках не хранятся модом — используются только механизмы ванили, поэтому реролл совместим с датапаками, меняющими торговлю.

## Установка

1. Установите [Fabric Loader](https://fabricmc.net/use/) для Minecraft 26.3.
2. Положите в `mods`:
   - [Fabric API](https://modrinth.com/mod/fabric-api) `0.165.2+26.3` или новее;
   - `reroll-plus-plus-2.0-beta.jar` (см. [Releases](../../releases)).
3. Мод нужен и на клиенте, и на сервере. В одиночной игре достаточно клиента.

## Сборка из исходников

Нужен **JDK 25**.

```bash
./gradlew build        # Linux/macOS
gradlew.bat build      # Windows
```

Готовый jar появится в `build/libs/reroll-plus-plus-2.0-beta.jar`.

> Первый запуск скачивает Minecraft, маппинги и декомпилирует исходники — это занимает несколько минут.

## Структура проекта

```
src/main/java/com/nullcoder/rerollplusplus/
├── RerollTradesPlus.java        # entrypoint: регистрация пакета и обработчика
├── RerollTradesPayload.java     # кастомный C2S-пакет
├── RerollCost.java              # таблица цен и списание изумрудов
├── RerollOdds.java              # шансы отказа торговца
└── mixin/                       # ванильная генерация сделок + реролл зачарований
src/client/java/com/nullcoder/rerollplusplus/client/
├── RerollButton.java            # кнопка ↻ на экране торговли
├── EnchantRerollButton.java     # кнопка ↻ на столе зачарований
├── RerollTradesPlusClient.java  # хоткей R
└── mixin/                       # встраивание кнопок в экраны
src/main/resources/assets/reroll_plus_plus/
├── icon.png                     # иконка мода
├── lang/{en_us,ru_ru}.json
└── textures/gui/reroll_button.png
```

## English (short)

**Reroll Plus Plus** adds a ↻ button to the villager trading screen and the enchanting table (Minecraft 26.3, Fabric). One click regenerates the villager's full offer set via vanilla `TradeSet` mechanics, or rerolls all three enchantment offers for 1 lapis lazuli. Rerolling costs emeralds — the price scales with the villager's trade level (1/2/4/8/16); wandering traders cost 1 emerald. Merchants may refuse (up to 20% at Master level, 10% for wandering traders) — nothing is charged on refusal. Hotkey: **R** (remappable). The buttons only appear when the server has the mod; all validation happens server-side.

## Лицензия

CC0-1.0 — делайте что хотите.
