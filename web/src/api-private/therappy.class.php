<?php

class therappy {

  private $db = null;

  function __construct() {
    // initialization code here
    $dbpw = '';
    $this->db = new mysqli('localhost', 'therappy', $dbpw, 'therappy');
  }

}
