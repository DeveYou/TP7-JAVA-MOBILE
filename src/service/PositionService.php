<?php
include_once __DIR__ . '/../dao/IDao.php';
include_once __DIR__ . '/../classe/Position.php';
include_once __DIR__ . '/../connexion/Connexion.php';

class PositionService implements IDao {
    private $connexion;

    public function __construct() {
        $this->connexion = (new Connexion())->getConnextion();
    }

    public function create($position) {
        $query = "INSERT INTO position (latitude, longitude, date, imei) VALUES (:lat, :lon, :dt, :imei)";
        $req = $this->connexion->prepare($query);
        try {
            $req->execute([
                ':lat'  => $position->getLatitude(),
                ':lon'  => $position->getLongitude(),
                ':dt'   => $position->getDate(),
                ':imei' => $position->getImei()
            ]);
        }catch (PDOException $e){
            throw new Exception('Database error: ' . $e->getMessage());
        }

    }

    public function getAll() {
        $query = "SELECT * FROM position";
        $req = $this->connexion->prepare($query);
        $req->execute();
        return $req->fetchAll(PDO::FETCH_ASSOC);
    }


    public function delete($obj) {}
    public function getById($obj) {}
    public function update($obj) {}
}