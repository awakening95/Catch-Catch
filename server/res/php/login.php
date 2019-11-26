<?php
    $host = 'localhost';
    $dbname = 'catch';  # DATABASE 이름

    $dsn = "mysql:host={$host};port=3306;dbname={$dbname};charset=utf8";

    $id = 'catch'; # MySQL 계정 아이디
    $password = '1q2w3e4r!'; # MySQL 계정 패스워드

    $android = strpos($_SERVER['HTTP_USER_AGENT'], "Android");

    try {
        if($android !== false) {
            $db = new PDO($dsn, $id, $password);
            $db -> setAttribute(PDO::ATTR_EMULATE_PREPARES, false); 
            $db -> setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

            $id = isset($_POST['id']) ? $_POST['id'] : '';
            $password = isset($_POST['password']) ? $_POST['password'] : '';

            if($id != '' and $password != '') {
                $query = "SELECT * FROM client_info where id=? and password=?";

                $stmt = $db->prepare($query);
                $stmt -> execute(array($id, $password));
                $result = $stmt -> rowCount();

                echo "$result";
            } else {
                echo "0";
            }
        } else {
            die();
        }
    } catch(PDOException $e) {
        die('Error: '.$e -> getMessage());
    }
?>
