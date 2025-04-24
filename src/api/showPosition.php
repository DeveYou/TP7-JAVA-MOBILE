<?php
// Set headers first to ensure proper content type
header('Content-Type: application/json');

// Define a root path if needed for consistent includes
define('ROOT_PATH', dirname(__DIR__));

try {
    // Only accept GET requests for retrieving data (more RESTful)
    if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
        http_response_code(405); // Method Not Allowed
        echo json_encode([
            'status' => 'error',
            'message' => 'Only GET method is allowed'
        ]);
        exit;
    }

    // Include files with full path for better reliability
    require_once ROOT_PATH . '/service/PositionService.php';

    // Get positions from service
    $positionService = new PositionService();
    $positions = $positionService->getAll();

    // Return successful response
    echo json_encode([
        'status' => 'success',
        'data' => [
            'positions' => $positions
        ]
    ]);

} catch (Exception $e) {
    // Handle any unexpected errors
    http_response_code(500);
    echo json_encode([
        'status' => 'error',
        'message' => 'An error occurred while retrieving positions',
        'error' => $e->getMessage() // Only include in development environment
    ]);
}