import json
import os
import socket
import threading

path = os.path.dirname(os.path.abspath(__file__))


def net(client_socket):
    recv_data = client_socket.recv(256).decode('UTF-8')

    try:
        data = recv_data.split(" ")

        ssid = data[0]
        key = data[1]
        id = data[2]

        client_socket.send(b'1')

        os.system("sudo service isc-dhcp-server stop")
        os.system("sudo service hostapd stop")

        f = open("/etc/network/interfaces", "w")
        f.write("# interfaces(5) file used by ifup(8) and ifdown(8)\n"
                "# Include files from /etc/network/interfaces.d:\n"
                "source-directory /etc/network/interfaces.d")
        f.close()

        os.system("sudo iwconfig wlan0 essid {} key s:{}".format(ssid, key))

        with open("{}/conf.json".format(path)) as json_read_file:
            data = json.load(json_read_file)
        json_read_file.close()

        # WiFi 연결 후 수정된 id, initial_setting 값 conf.json 파일에 저장
        data["id"] = id
        data["initial_setting"] = "true"

        with open("{}/conf.json".format(path), 'w') as json_write_file:
            json.dump(data, json_write_file)
        json_write_file.close()

        os.system("sudo reboot")

    except Exception:
        client_socket.send(b'0')


if __name__ == "__main__":
    with open("{}/conf.json".format(path)) as json_read_file:
        data = json.load(json_read_file)
    json_read_file.close()

    # 초기 설정 확인
    initial_setting = data["initial_setting"]

    if initial_setting == "false":
        os.system("sudo service isc-dhcp-server start")
        os.system("sudo service hostapd start")

        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.bind(('10.10.0.1', 9700))
        server_socket.listen(5)

        while True:
            client_socket, addr = server_socket.accept()
            t = threading.Thread(target=net, args=(client_socket,))
            t.start()

    elif initial_setting == "true":
        pass
        # os.system("python3 {}/network/server/client.py &".format(path))
        # os.system("python3 {}/network/game/server.py &".format(path))