# 1. Cấp Night Vision vĩnh viễn cho toàn server
effect give @a night_vision infinite 0 true

# 2. Bộ giáp da nhuộm màu vàng + tên Chill MC vàng rực lửa
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run give @s leather_helmet{display:{color:16770560,Name:'{"text":"Chill MC","color":"gold","bold":true,"italic":false}'}} 1
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run give @s leather_chestplate{display:{color:16770560,Name:'{"text":"Chill MC","color":"gold","bold":true,"italic":false}'}} 1
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run give @s leather_leggings{display:{color:16770560,Name:'{"text":"Chill MC","color":"gold","bold":true,"italic":false}'}} 1
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run give @s leather_boots{display:{color:16770560,Name:'{"text":"Chill MC","color":"gold","bold":true,"italic":false}'}} 1

# 3. Bộ vũ khí & công cụ sắt + tên Chill MC vàng rực lửa
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run give @s iron_sword{display:{Name:'{"text":"Chill MC","color":"gold","bold":true,"italic":false}'}} 1
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run give @s iron_pickaxe{display:{Name:'{"text":"Chill MC","color":"gold","bold":true,"italic":false}'}} 1
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run give @s iron_axe{display:{Name:'{"text":"Chill MC","color":"gold","bold":true,"italic":false}'}} 1
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run give @s iron_shovel{display:{Name:'{"text":"Chill MC","color":"gold","bold":true,"italic":false}'}} 1

# 4. 32 Bít tết (giữ nguyên tên gốc)
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run give @s cooked_beef 32

# 5. Nhớ người chơi vĩnh viễn & reset bộ đếm bấm chuột
execute at @e[tag=tt] as @a[distance=..4,scores={bam=1..},tag=!da_nhan] run tag @s add da_nhan
scoreboard players set @a bam 0
# Chuyển người chơi thường ở vùng Spawn (bán kính 50 block) sang Adventure để không đập/đặt được khối
gamemode adventure @a[x=-195,y=63,z=41,distance=..50,gamemode=survival,tag=!admin]

# Ra khỏi vùng Spawn -> Trả về Survival bình thường
gamemode survival @a[x=-195,y=63,z=41,distance=51..70,gamemode=adventure,tag=!admin]

effect give @a[x=-195,y=63,z=41,distance=..50] resistance 2 255 true
