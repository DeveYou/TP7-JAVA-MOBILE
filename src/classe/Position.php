<?php
class Position {
    private $id;
    private $latitude;
    private $longitude;
    private $date;
    private $imei;

    public function __construct($id, $latitude, $longitude, $date, $imei) {
        $this->id = $id;
        $this->latitude = $latitude;
        $this->longitude = $longitude;
        $this->date = $date;
        $this->imei = $imei;
    }
    // Getters et setters...
    public function getId() { return $this->id; }
    public function getLatitude() { return $this->latitude; }
    public function getLongitude() { return $this->longitude; }
    public function getDate() { return $this->date; }
    public function getImei() { return $this->imei; }
    public function setId($id) { $this->id = $id; }
    public function setLatitude($latitude) { $this->latitude = $latitude; }
    public function setLongitude($longitude) { $this->longitude = $longitude; }
    public function setDate($date) { $this->date = $date; }
    public function setImei($imei) { $this->imei = $imei; }
}