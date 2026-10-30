#!/usr/bin/env python3
"""Interface strings of the Rig, Wheel, Deck boot and HUD, and the stat labels of the implant details panel, in English
and Russian. Merges them into both language files (existing keys are overwritten, others kept).
Run from the repository root:  python3 scripts/ui_lang.py"""
import json
import os

LANG = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "cyberimplants", "lang")

UI = {  # key: (english, russian)
    "cyberimplants.arm.haptic_on": ("Haptic precision: on (empty hand mines with silk touch)",
                                    "Тактильная точность: вкл. (пустая рука добывает с шёлковым касанием)"),
    "cyberimplants.arm.haptic_off": ("Haptic precision: off", "Тактильная точность: выкл."),
    "entity.cyberimplants.rig_seat": ("Rig Seat", "Сиденье кресла"),
    "cyberimplants.rig.cannot_seat": ("The Rig could not strap you in.", "Кресло не смогло вас зафиксировать."),
    "cyberimplants.rig.abort.moved": ("Operation aborted: the patient left the chair.", "Операция прервана: пациент покинул кресло."),
    "cyberimplants.rig.header.implants": ("IMPLANTS  %s", "ИМПЛАНТЫ  %s"),
    "cyberimplants.rig.inventory_empty": ("No implants in your inventory. Craft some on a Create production line, then come back.",
                                          "В инвентаре нет имплантов. Соберите их на производственной линии Create и возвращайтесь."),
    "cyberimplants.rig.no_fit": ("no slot", "нет слота"),
    "cyberimplants.rig.slot_name": ("%s, slot %s", "%s, слот %s"),
    "cyberimplants.rig.load.short": ("Load %s / %s", "Нагрузка %s / %s"),
    "cyberimplants.rig.telemetry.rpm": ("%s RPM", "%s об/мин"),
    "cyberimplants.rig.telemetry.offline": ("No power", "Нет вращения"),
    "cyberimplants.rig.telemetry.gel": ("%s mB %s", "%s мБ %s"),
    "cyberimplants.rig.telemetry.no_gel": ("No gel", "Нет геля"),
    "cyberimplants.rig.telemetry.locked": ("Locked", "Заблокировано"),
    "cyberimplants.rig.telemetry.restrained": ("Restrained", "Зафиксирован"),
    "cyberimplants.rig.action.install": ("Install", "Установить"),
    "cyberimplants.rig.action.extract": ("Extract", "Извлечь"),
    "cyberimplants.rig.action.confirm": ("Confirm extraction?", "Точно извлечь?"),
    "cyberimplants.rig.action.select": ("Select an implant", "Выберите имплант"),
    "cyberimplants.rig.action.busy": ("Operating...", "Идёт операция..."),
    "cyberimplants.rig.hint.select": ("Pick an implant on the left, or click a node to inspect it.",
                                      "Выберите имплант слева или щёлкните по узлу, чтобы его осмотреть."),
    "cyberimplants.rig.hint.place": ("Click a glowing node to start the operation.", "Щёлкните по светящемуся узлу, чтобы начать операцию."),
    "cyberimplants.rig.hint.operating": ("Hold still. The Rig has you.", "Не двигайтесь. Кресло держит вас."),
    "cyberimplants.rig.hint.drag": ("Drag the hologram to turn it. Double-click an implant to install it where it fits.",
                                    "Тяните голограмму, чтобы повернуть. Двойной щелчок по импланту ставит его в подходящий слот."),
    "cyberimplants.rig.hint.no_slot": ("This implant has no free slot that accepts it right now.",
                                       "Сейчас для этого импланта нет свободного подходящего слота."),
    "cyberimplants.rig.hint.inspect": ("Click to inspect or extract", "Щелчок: осмотреть или извлечь"),
    "cyberimplants.rig.hint.click_install": ("Click to install here", "Щёлкните, чтобы установить сюда"),
    "cyberimplants.rig.status.starting": ("Strapping in and starting the operation...", "Фиксация и начало операции..."),
    "cyberimplants.rig.details.overview": ("Body overview", "Обзор тела"),
    "cyberimplants.rig.details.installed": ("Implants installed", "Установлено имплантов"),
    "cyberimplants.rig.details.capacity": ("Neural Capacity", "Нейроёмкость"),
    "cyberimplants.rig.details.battery": ("Battery", "Батарея"),
    "cyberimplants.rig.details.recharge": ("Metabolic recharge", "Метаболическая зарядка"),
    "cyberimplants.rig.details.core_first": ("No Cybernetic Core yet. Install the Core first: nothing else works without it.",
                                             "Кибернетического ядра ещё нет. Сначала установите ядро: без него ничего не работает."),
    "cyberimplants.rig.details.stats": ("Stats", "Характеристики"),
    "cyberimplants.rig.details.now": ("vs installed", "к установленному"),
    "cyberimplants.rig.details.next": ("next tier", "следующий тир"),
    "cyberimplants.rig.details.unlocks": ("Features", "Возможности"),
    "cyberimplants.rig.details.next_unlock": ("Mk. %s", "Мк. %s"),
    "cyberimplants.rig.details.operation": ("Operation", "Операция"),
    "cyberimplants.rig.details.instant": ("Instant (Layer 1 rules)", "Мгновенно (правила слоя 1)"),
    "cyberimplants.rig.details.time": ("Duration", "Длительность"),
    "cyberimplants.rig.details.speed": ("Shaft speed", "Скорость вала"),
    "cyberimplants.rig.stat.load": ("Neural Load", "Нейронагрузка"),
    "cyberimplants.rig.stat.charge": ("Charge cost", "Расход заряда"),
    "cyberimplants.rig.stat.cooldown": ("Cooldown", "Перезарядка"),
    "cyberimplants.rig.op.install": ("Installing %s", "Установка: %s"),
    "cyberimplants.rig.op.remove": ("Extracting %s", "Извлечение: %s"),
    "cyberimplants.rig.op.left": ("%s s left", "ещё %s с"),
    "cyberimplants.rig.stage.0": ("Sterilising the field", "Стерилизация поля"),
    "cyberimplants.rig.stage.1": ("Opening a neural port", "Открытие нейропорта"),
    "cyberimplants.rig.stage.splice": ("Splicing the implant", "Вживление импланта"),
    "cyberimplants.rig.stage.detach": ("Detaching the implant", "Отсоединение импланта"),
    "cyberimplants.rig.stage.3": ("Calibrating feedback", "Калибровка отклика"),
    "cyberimplants.rig.stage.4": ("Sealing tissue", "Закрытие тканей"),
    "cyberimplants.rig.outcome.installed": ("Implant online", "Имплант в сети"),
    "cyberimplants.rig.outcome.removed": ("Extraction complete", "Извлечение завершено"),
    "cyberimplants.rig.outcome.aborted": ("Operation aborted", "Операция прервана"),
    "cyberimplants.rig.boot.link": ("> link rig @ %s", "> связь с креслом @ %s"),
    "cyberimplants.rig.boot.handshake": ("> handshake ............ ok", "> рукопожатие ......... ок"),
    "cyberimplants.rig.boot.scan": ("> scanning patient", "> сканирование пациента"),
    "cyberimplants.rig.boot.map": ("> mapping body slots", "> разметка слотов тела"),
    "cyberimplants.rig.boot.ready": ("> body map ready", "> карта тела готова"),
    "cyberimplants.deck.boot.bios": ("> synapse bios v2", "> synapse bios v2"),
    "cyberimplants.deck.boot.isa": ("> probing processor", "> опрос процессора"),
    "cyberimplants.deck.boot.mount": ("> mounting storage", "> подключение хранилища"),
    "cyberimplants.deck.boot.ready": ("> deck online", "> дека в сети"),
}

STATS = {  # cyberimplants.stat.<key>: (english, russian)
    "absorb_share": ("Damage absorbed", "Поглощение урона"),
    "accel_bonus": ("Acceleration", "Ускорение"),
    "aggro_drop_ticks": ("Aggro drop", "Сброс агрессии"),
    "air_control": ("Air control", "Управление в воздухе"),
    "air_jumps": ("Air jumps", "Прыжки в воздухе"),
    "alarms": ("Alarms", "Сигналы тревоги"),
    "armor": ("Armour", "Броня"),
    "attack_damage": ("Attack damage", "Урон атаки"),
    "attack_speed": ("Attack speed", "Скорость атаки"),
    "balanced": ("All-round resistance", "Общая защита"),
    "base_threads": ("Base threads", "Базовые потоки"),
    "battery": ("Battery", "Батарея"),
    "bilateral_break_speed": ("Two-arm mining bonus", "Бонус добычи двумя руками"),
    "blunt_flat": ("Blunt damage blocked", "Блок дробящего урона"),
    "boost": ("Speed boost", "Ускорение"),
    "boost_ticks": ("Boost duration", "Длительность ускорения"),
    "bound_ports": ("Bound ports", "Привязанные порты"),
    "break_speed": ("Mining speed", "Скорость добычи"),
    "broadcast_channels": ("Broadcast channels", "Каналы трансляции"),
    "buff_speed": ("Dash speed buff", "Бонус скорости рывка"),
    "buff_ticks": ("Buff duration", "Длительность бонуса"),
    "capacity": ("Capacity", "Ёмкость"),
    "cast_range": ("Cast range", "Дальность применения"),
    "charge_per_hp": ("Charge per HP", "Заряд за 1 HP"),
    "charges": ("Charges", "Заряды"),
    "climb_speed": ("Climb speed", "Скорость лазания"),
    "condition_slots": ("Condition slots", "Слоты условий"),
    "counter_bonus": ("Counter-attack bonus", "Бонус контратаки"),
    "crash_ticks": ("Crash afterwards", "Упадок после"),
    "cycles_per_tick": ("Cycles per tick", "Циклов за тик"),
    "damage": ("Damage", "Урон"),
    "dash_blocks": ("Dash length", "Длина рывка"),
    "decay_per_second": ("Gauge decay", "Спад шкалы"),
    "detect_moving": ("Seen while moving", "Заметность в движении"),
    "detect_still": ("Seen while still", "Заметность на месте"),
    "detection_ticks": ("Reaction time", "Время реакции"),
    "direct_range": ("Direct range", "Прямая дальность"),
    "distance": ("Distance", "Дистанция"),
    "dual_attack_speed": ("Dual-wield speed", "Скорость двух клинков"),
    "duration_ticks": ("Duration", "Длительность"),
    "emergency_cooldown_ticks": ("Emergency cooldown", "Аварийная перезарядка"),
    "energy_per_block": ("Energy per block fallen", "Энергия за блок падения"),
    "extra_threads": ("Extra threads", "Доп. потоки"),
    "fall_reduction": ("Fall damage reduction", "Снижение урона от падения"),
    "fill_rate": ("Fill rate", "Скорость заполнения"),
    "freeze_time_scale": ("Time in frozen field", "Время в замороженном поле"),
    "full_charge_ticks": ("Full pressure after", "Полное давление за"),
    "harvest_level": ("Harvest level", "Уровень добычи"),
    "heal_per_second": ("Healing", "Лечение"),
    "health_restored": ("Health restored", "Восстановление здоровья"),
    "horizon_ticks": ("Prediction horizon", "Горизонт прогноза"),
    "indicators": ("Indicators", "Индикаторы"),
    "internal_cooldown_ticks": ("Internal cooldown", "Внутренняя перезарядка"),
    "isa": ("Instruction set", "Набор инструкций"),
    "item_use_speed": ("Item use speed", "Скорость использования"),
    "jump_height": ("Jump height", "Высота прыжка"),
    "jump_strength": ("Jump strength", "Сила прыжка"),
    "kb": ("Size", "Объём"),
    "knockback": ("Knockback", "Отбрасывание"),
    "knockback_resistance": ("Knockback resistance", "Сопротивление отбрасыванию"),
    "latency_ticks": ("Neural latency", "Нейрозадержка"),
    "lockout_ticks": ("Lockout after damage", "Пауза после урона"),
    "log_lines": ("Log lines", "Строк журнала"),
    "machinery": ("Machinery protection", "Защита от механизмов"),
    "macro_keys": ("Macro keys", "Макроклавиши"),
    "magazine": ("Magazine", "Магазин"),
    "max_coprocessors": ("Max coprocessors", "Макс. сопроцессоров"),
    "max_health": ("Max health", "Макс. здоровье"),
    "max_horizontal": ("Max leap distance", "Макс. дальность прыжка"),
    "max_markers": ("Max markers", "Макс. меток"),
    "max_projectiles": ("Tracked projectiles", "Отслеживаемые снаряды"),
    "max_tags": ("Max tags", "Макс. целей"),
    "max_vertical": ("Max leap height", "Макс. высота прыжка"),
    "min_every_ticks": ("Shortest EVERY", "Минимальный EVERY"),
    "module_sockets": ("Module sockets", "Гнёзда модулей"),
    "movement_penalty": ("Movement penalty", "Штраф к движению"),
    "overlay_upkeep_per_second": ("Overlay upkeep", "Расход оверлея"),
    "overload_margin": ("Overload margin", "Запас перегрузки"),
    "persistence_ticks": ("Tag lasts", "Метка держится"),
    "radius": ("Radius", "Радиус"),
    "range": ("Range", "Дальность"),
    "reach": ("Extra reach", "Доп. досягаемость"),
    "recharge_delay_ticks": ("Recharge delay", "Задержка зарядки"),
    "recharge_per_second": ("Recharge", "Зарядка"),
    "recharge_rate": ("Recharge rate", "Скорость зарядки"),
    "recharge_ticks": ("Recharge time", "Время зарядки"),
    "recovery_cap": ("Recovery cap", "Предел восстановления"),
    "reduction": ("Damage reduction", "Снижение урона"),
    "reload_ticks": ("Reload", "Перезарядка магазина"),
    "resistance_amplifier": ("Resistance level", "Уровень сопротивления"),
    "resistance_ticks": ("Resistance duration", "Длительность сопротивления"),
    "response_slots": ("Response slots", "Слоты реакций"),
    "reveal_ticks": ("Reveal duration", "Длительность подсветки"),
    "safe_fall": ("Safe fall", "Безопасное падение"),
    "shield_raise_ticks": ("Shield raise time", "Подъём щита"),
    "sneak_detection_reduction": ("Sneak stealth", "Скрытность крадучись"),
    "sneak_speed": ("Sneak speed", "Скорость крадучись"),
    "sockets": ("Sockets", "Гнёзда"),
    "specialised": ("Specialised resistance", "Узкая защита"),
    "sprint_exhaustion": ("Sprint exhaustion", "Истощение от бега"),
    "sprint_gain_per_second": ("Gauge gain while sprinting", "Рост шкалы при беге"),
    "sprint_speed": ("Sprint speed", "Скорость бега"),
    "stamina_ticks": ("Grip stamina", "Выносливость хвата"),
    "step_height": ("Step height", "Высота шага"),
    "strength": ("Strength", "Сила"),
    "suppression_level": ("Noise suppression", "Подавление шума"),
    "time_scale": ("Time in field", "Время в поле"),
    "toughness": ("Armour toughness", "Прочность брони"),
    "tracking_radius": ("Tracking radius", "Радиус отслеживания"),
    "trigger_damage": ("Trigger damage", "Порог урона"),
    "trigger_health": ("Trigger health", "Порог здоровья"),
    "turn_speed_kept": ("Speed kept in turns", "Скорость в поворотах"),
    "unarmed_multiplier": ("Unarmed damage", "Урон без оружия"),
    "upkeep_per_second": ("Upkeep", "Расход"),
    "velocity": ("Projectile speed", "Скорость снаряда"),
    "virtual_ports": ("Virtual ports", "Виртуальные порты"),
    "walk_speed": ("Walk speed", "Скорость ходьбы"),
    "wall_impact_reduction": ("Wall impact reduction", "Снижение удара о стену"),
    "wall_penetration": ("Sees through walls", "Видит сквозь стены"),
    "wall_walk_speed": ("Wall walk speed", "Скорость по стенам"),
    "wear_reduction": ("Armour wear reduction", "Снижение износа брони"),
    "weight": ("Weight", "Вес"),
    "window_ticks": ("Dodge window", "Окно уклонения"),
}

MANUAL_RIG = (
    "# Building it\n"
    "- Implantation Rig: iron sheets, a Mechanical Servo, a Civilian Casing, an Andesite Casing, a shaft and a cogwheel. It is a surgical stool: the shaft goes into the gearbox at its back.\n"
    "- Connect a shaft to it. The Rig needs rotation to work.\n"
    "- Bio-Interface Gel: Mechanical Mixer (no heat), 250 mB water + slime ball + bone meal + sugar.\n"
    "- Neural Gel (for Tier IV and V): heated Mixer, 250 mB Bio-Interface Gel + 50 mB Nanite Suspension (superheated Mixer: 250 mB Bio-Interface Gel + 2 Silica Dust + redstone + 3 iron nuggets).\n"
    "- Fill the Rig's tank (1000 mB) with a bucket or a pipe.\n\n"
    "# Requirements by implant tier\n"
    "|Tier I / II / III / IV / V\n"
    "|Time:  5 / 8 / 12 / 16 / 20 seconds\n"
    "|Speed: 16 / 32 / 64 / 128 / 256 RPM\n"
    "|Gel:   250 mB (Bio-Interface for I to III, Neural for IV and V)\n\n"
    "# The screen\n"
    "- Right-click the Rig. Left: the implants in your inventory. Centre: a hologram of your skeleton with a node on every body slot. Right: details of whatever you point at.\n"
    "- The top bar shows the Rig's speed and gel, and your Neural Load. With an implant selected, a ghost segment shows the Load it would add.\n"
    "- Pick an implant: the slots it fits glow green. Click one to start. Double-clicking an implant installs it in the first slot that fits.\n"
    "- Click an installed node to inspect it, then Extract (click twice to confirm). Drag the hologram to turn it.\n"
    "- The details panel plays a preview of the implant's effect and lists its stats, compared with the one you have installed or with the next tier, its features and what the operation needs (a red cross marks what is missing).\n\n"
    "# The operation\n"
    "- When the operation starts the Rig straps you onto its stool: you cannot walk, jump or stand up until it finishes. The screen and the top of your HUD show the progress.\n"
    "- Install the Core first. Removed implants return to your inventory with their tier.\n\n"
    "! The operation aborts if the Rig slows below the needed RPM, runs out of gel, or you leave the chair some other way (an ender pearl, for instance). You take 4 damage and the implant stays where it was.\n\n"
    "> A redstone signal on the Rig locks it (useful for a clinic door). A comparator reads the operation progress.",
    "# Постройка\n"
    "- Кресло имплантации: железные листы, механический сервопривод, гражданский корпус, андезитовый корпус, вал и шестерня. Это хирургический табурет: вал входит в редуктор сзади.\n"
    "- Подведите к нему вал. Креслу нужно вращение.\n"
    "- Биоинтерфейсный гель: механический миксер (без нагрева), 250 мБ воды + слизь + костная мука + сахар.\n"
    "- Нейрогель (для IV и V тира): миксер с нагревом, 250 мБ биоинтерфейсного геля + 50 мБ нанитовой суспензии (миксер с сильным нагревом: 250 мБ биоинтерфейсного геля + 2 кремнезёма + редстоун + 3 железных самородка).\n"
    "- Залейте бак кресла (1000 мБ) вёдрами или трубой.\n\n"
    "# Требования по тиру импланта\n"
    "|Тир:       I / II / III / IV / V\n"
    "|Время:     5 / 8 / 12 / 16 / 20 секунд\n"
    "|Скорость:  16 / 32 / 64 / 128 / 256 об/мин\n"
    "|Гель:      250 мБ (биоинтерфейсный для I-III, нейрогель для IV-V)\n\n"
    "# Экран\n"
    "- ПКМ по креслу. Слева - импланты из инвентаря. В центре - голограмма вашего скелета с узлом на каждом слоте тела. Справа - подробности о том, на что вы указываете.\n"
    "- Верхняя полоса показывает скорость и гель кресла и вашу нейронагрузку. Если выбран имплант, полупрозрачный отрезок показывает, сколько нагрузки он добавит.\n"
    "- Выберите имплант: подходящие слоты загорятся зелёным. Щёлкните по одному, чтобы начать. Двойной щелчок по импланту ставит его в первый подходящий слот.\n"
    "- Щёлкните по занятому узлу, чтобы осмотреть имплант, затем \"Извлечь\" (два щелчка для подтверждения). Голограмму можно поворачивать мышью.\n"
    "- Панель подробностей показывает анимацию эффекта импланта, его характеристики в сравнении с установленным или со следующим тиром, возможности и требования операции (красный крестик - чего не хватает).\n\n"
    "# Операция\n"
    "- Когда операция начинается, кресло фиксирует вас на табурете: ходить, прыгать и вставать нельзя до конца. Ход операции виден на экране и вверху интерфейса.\n"
    "- Ядро ставится первым. Снятые импланты возвращаются в инвентарь вместе с тиром.\n\n"
    "! Операция прерывается, если кресло замедлилось ниже нужной скорости, кончился гель или вы покинули кресло иначе (например, жемчугом Эндера). Вы получаете 4 урона, имплант остаётся на прежнем месте.\n\n"
    "> Сигнал красного камня блокирует кресло (удобно для двери клиники). Компаратор показывает ход операции.",
)


def main():
    for lang, idx in (("en_us", 0), ("ru_ru", 1)):
        path = os.path.join(LANG, lang + ".json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        for key, texts in UI.items():
            data[key] = texts[idx]
        for key, texts in STATS.items():
            data["cyberimplants.stat." + key] = texts[idx]
        data["cyberimplants.manual.rig.body"] = MANUAL_RIG[idx]
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    main()
