# Kamikaze Drones (NeoForge 1.21.1)

Сборка jar (нужны JDK 21 и Gradle 8.10+ и интернет):

    gradle build

Готовый файл: build/libs/dronemod-1.0.0.jar -> положить в папку mods (NeoForge 1.21.1).

Если Gradle ругается на версию NeoForge, поменяй version в build.gradle на любую 21.1.x
(список: https://projects.neoforged.net/neoforged/neoforge).

Звуки: src/main/resources/assets/dronemod/sounds (drone_flight.ogg - полёт, drone_explosion.ogg - взрыв).
Настройки (частота налётов, мощность взрыва, размер постройки) - константы в начале DroneMod.java и DroneEntity.java.
Проверка: яйцо призыва в креативе или /summon dronemod:drone
