<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

$documentRoot = (string)($_SERVER['DOCUMENT_ROOT'] ?? __DIR__);
$configFile = dirname($documentRoot, 2) . '/nexvo-private/config.php';
if (!is_file($configFile)) {
    http_response_code(503);
    echo json_encode(['ok' => false, 'error' => 'server_not_configured']);
    exit;
}

$config = require $configFile;

function respond(int $status, array $body): never {
    http_response_code($status);
    echo json_encode($body, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function bearerToken(): string {
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    return preg_match('/^Bearer\s+(.+)$/i', $header, $m) ? trim($m[1]) : '';
}

function bodyJson(): array {
    $raw = file_get_contents('php://input');
    $data = json_decode($raw ?: '{}', true);
    return is_array($data) ? $data : [];
}

$path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$basePath = rtrim(str_replace('\\', '/', dirname((string)($_SERVER['SCRIPT_NAME'] ?? ''))), '/');
if ($basePath !== '' && $basePath !== '/' && str_starts_with($path, $basePath)) {
    $path = substr($path, strlen($basePath)) ?: '/';
}
$method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

if ($path === '/' || $path === '/health') {
    try {
        $healthPdo = new PDO(
            'mysql:host=' . $config['db_host'] . ';dbname=' . $config['db_name'] . ';charset=utf8mb4',
            $config['db_user'],
            $config['db_pass'],
            [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
        );
        $healthPdo->query('SELECT 1');
        respond(200, ['ok' => true, 'database' => true, 'service' => 'nexvo-aprs-api', 'version' => '1.0.0', 'time' => gmdate('c')]);
    } catch (Throwable $e) {
        error_log('Nexvo APRS health: ' . $e->getMessage());
        respond(503, ['ok' => false, 'database' => false, 'error' => 'database_unavailable']);
    }
}

if (!hash_equals((string)$config['api_token'], bearerToken())) {
    respond(401, ['ok' => false, 'error' => 'unauthorized']);
}

try {
    $pdo = new PDO(
        'mysql:host=' . $config['db_host'] . ';dbname=' . $config['db_name'] . ';charset=utf8mb4',
        $config['db_user'],
        $config['db_pass'],
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC]
    );

    $pdo->exec("CREATE TABLE IF NOT EXISTS messages (
        id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        sender VARCHAR(20) NOT NULL,
        recipient VARCHAR(20) NOT NULL,
        body VARCHAR(512) NOT NULL,
        status ENUM('queued','delivered','failed') NOT NULL DEFAULT 'queued',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        delivered_at TIMESTAMP NULL DEFAULT NULL,
        INDEX idx_recipient_status (recipient, status),
        INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    $pdo->exec("CREATE TABLE IF NOT EXISTS devices (
        id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        callsign VARCHAR(20) NOT NULL,
        fcm_token VARCHAR(255) NOT NULL,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uniq_callsign_token (callsign, fcm_token)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    if ($path === '/v1/messages' && $method === 'POST') {
        $data = bodyJson();
        $from = strtoupper(trim((string)($data['from'] ?? '')));
        $to = strtoupper(trim((string)($data['to'] ?? '')));
        $body = trim((string)($data['body'] ?? ''));
        if (!preg_match('/^[A-Z0-9-]{3,20}$/', $from) || !preg_match('/^[A-Z0-9-]{3,20}$/', $to) || $body === '' || mb_strlen($body) > 512) {
            respond(422, ['ok' => false, 'error' => 'invalid_message']);
        }
        $stmt = $pdo->prepare('INSERT INTO messages (sender, recipient, body) VALUES (?, ?, ?)');
        $stmt->execute([$from, $to, $body]);
        respond(201, ['ok' => true, 'id' => (int)$pdo->lastInsertId(), 'status' => 'queued']);
    }

    if ($path === '/v1/messages' && $method === 'GET') {
        $call = strtoupper(trim((string)($_GET['callsign'] ?? '')));
        if (!preg_match('/^[A-Z0-9-]{3,20}$/', $call)) respond(422, ['ok' => false, 'error' => 'invalid_callsign']);
        $stmt = $pdo->prepare('SELECT id, sender, recipient, body, status, created_at, delivered_at FROM messages WHERE sender = ? OR recipient = ? ORDER BY id DESC LIMIT 100');
        $stmt->execute([$call, $call]);
        respond(200, ['ok' => true, 'messages' => $stmt->fetchAll()]);
    }

    if ($path === '/v1/devices/register' && $method === 'POST') {
        $data = bodyJson();
        $call = strtoupper(trim((string)($data['callsign'] ?? '')));
        $token = trim((string)($data['fcm_token'] ?? ''));
        if (!preg_match('/^[A-Z0-9-]{3,20}$/', $call) || strlen($token) < 20 || strlen($token) > 255) {
            respond(422, ['ok' => false, 'error' => 'invalid_device']);
        }
        $stmt = $pdo->prepare('INSERT INTO devices (callsign, fcm_token) VALUES (?, ?) ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP');
        $stmt->execute([$call, $token]);
        respond(200, ['ok' => true]);
    }

    respond(404, ['ok' => false, 'error' => 'not_found']);
} catch (Throwable $e) {
    error_log('Nexvo APRS API: ' . $e->getMessage());
    respond(500, ['ok' => false, 'error' => 'server_error']);
}
