<?php
define('ROOT_PATH', __DIR__ . '/..');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    header('Content-Type: application/json');

    // Chargement des classes
    require_once ROOT_PATH . '/service/PositionService.php';
    require_once ROOT_PATH . '/classe/Position.php';

    // Get raw JSON data
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);

    // Check if JSON decoding was successful
    if (json_last_error() !== JSON_ERROR_NONE) {
        http_response_code(400);
        echo json_encode([
            'status' => 'error',
            'message' => 'Invalid JSON data'
        ]);
        exit;
    }

    // Validate required fields exist
    if (!isset($data['latitude']) || !isset($data['longitude']) || !isset($data['date']) || !isset($data['imei'])) {
        http_response_code(400);
        echo json_encode([
            'status' => 'error',
            'message' => 'Missing required fields'
        ]);
        exit;
    }

    // Validate data
    $latitude = filter_var($data['latitude'], FILTER_VALIDATE_FLOAT);
    $longitude = filter_var($data['longitude'], FILTER_VALIDATE_FLOAT);
    $dateStr = filter_var($data['date'], FILTER_SANITIZE_STRING);
    $imei = filter_var($data['imei'], FILTER_SANITIZE_STRING);

    // Check for valid values
    if ($latitude === false || $longitude === false || !$dateStr || !$imei) {
        http_response_code(400);
        echo json_encode([
            'status' => 'error',
            'message' => 'Invalid input data'
        ]);
        exit;
    }

    // Validate date format (YYYY-MM-DD HH:MM:SS)
    $date = DateTime::createFromFormat('Y-m-d H:i:s', $dateStr);
    if ($date === false) {
        http_response_code(400);
        echo json_encode([
            'status' => 'error',
            'message' => 'Invalid date format. Expected YYYY-MM-DD HH:MM:SS'
        ]);
        exit;
    }

    try {
        $service = new PositionService();
        $position = new Position(0, $latitude, $longitude, $dateStr, $imei);
        $service->create($position);

        echo json_encode(['status' => 'success']);
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'status' => 'error',
            'message' => $e->getMessage()
        ]);
    }
}