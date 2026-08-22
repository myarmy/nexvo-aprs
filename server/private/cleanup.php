<?php
declare(strict_types=1);

$config = require __DIR__ . '/config.php';
$pdo = new PDO(
    'mysql:host=' . $config['db_host'] . ';dbname=' . $config['db_name'] . ';charset=utf8mb4',
    $config['db_user'],
    $config['db_pass'],
    [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
);
$pdo->exec("DELETE FROM messages WHERE created_at < (UTC_TIMESTAMP() - INTERVAL 90 DAY)");
