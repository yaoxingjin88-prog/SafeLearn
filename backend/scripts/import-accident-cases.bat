@echo off
chcp 65001 >nul
set SQL_FILE=%~dp0..\src\main\resources\data\accident_cases_index.sql
mysql -u root -p123456 --default-character-set=utf8mb4 safelearn -e "DELETE FROM accident_cases WHERE id NOT IN ('40000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002');"
mysql -u root -p123456 --default-character-set=utf8mb4 safelearn < "%SQL_FILE%"
mysql -u root -p123456 --default-character-set=utf8mb4 safelearn -e "SELECT COUNT(*) AS total_cases FROM accident_cases; SELECT id, title FROM accident_cases ORDER BY date DESC LIMIT 5;"
