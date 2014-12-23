<?php
  // This file calls the therappy class to process data

  try {
    // Load the therappy class
    require('../api-private/therappy.class.php');

    // Create new instance and process request
    $app = new therappy();
    $app->processRequest($_SERVER['QUERY_STRING'], $_POST);

  } catch (Exception $e) {
    // If there is a runtime error that isn't already handled, return error message in JSON format
    echo json_encode(array('status'=>1, 'error'=>$e->message));
  }
