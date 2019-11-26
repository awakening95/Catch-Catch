<?php
    $host = 'localhost';
    $dbname = 'catch';

    $dsn = "mysql:host={$host};port=3306;dbname={$dbname};charset=utf8";

    $id = 'catch';
    $password = '1q2w3e4r!';

    $android = strpos($_SERVER['HTTP_USER_AGENT'], "Android");

    try {
        if($android !== false) {
            $db = new PDO($dsn, $id, $password);
            $db->setAttribute(PDO::ATTR_EMULATE_PREPARES, false); 
            $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

            $id = $_POST['id'];

            $query = "SELECT * FROM client_info where id=:id";

            $stmt = $db->prepare($query);
            $stmt -> bindParam(':id', $id);

            if($stmt -> execute()) {
                $result = $stmt -> rowCount();
                echo "$result";
            } else {
                echo "1";
            }
        } else {
            die();
        }
    } catch(PDOException $e) {
        die('Error: '.$e -> getMessage());
    }
?>

