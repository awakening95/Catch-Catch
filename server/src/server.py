import socket
import threading
import pymysql
import os
import datetime
import time
import subprocess

# From CatchCatch Module
server_socket_9500 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket_9500.bind(('172.31.47.173', 9500))
server_socket_9500.listen(0)

server_socket_9501 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket_9501.bind(('172.31.47.173', 9501))
server_socket_9501.listen(0)

server_socket_9502 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket_9502.bind(('172.31.47.173', 9502))
server_socket_9502.listen(0)

# From CatchCatch App
server_socket_9600 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket_9600.bind(('172.31.47.173', 9600))
server_socket_9600.listen(0)

server_socket_9601 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket_9601.bind(('172.31.47.173', 9601))
server_socket_9601.listen(0)

server_socket_9602 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket_9602.bind(('172.31.47.173', 9602))
server_socket_9602.listen(0)

client_info = {}  # {ID, Socket}
client_info_for_voice = {}  # {ID, Socket}, 음성 데이터 전달을 위한 소켓 저장


def from_port_9500():  # CatchCatch 앱으로부터 받은 명령을 CatchCatch 모듈로 전달하기 위해 CatchCatch 모듈로 부터 Socket 획득
    client_socket, addr = server_socket_9500.accept()
    t = threading.Thread(target=from_port_9500)
    t.start()

    recv_data = client_socket.recv(128)
    id = recv_data.decode().split(" ")[0]
    client_info[id] = client_socket


def from_port_9501():  # CatchCatch 모듈이 접근할 때마다 데이터베이스에 접근하여 해당 ID의 access_time 갱신
    client_socket, addr = server_socket_9501.accept()
    t = threading.Thread(target=from_port_9501)
    t.start()

    recv_data = client_socket.recv(128)
    id = recv_data.decode().split(" ")[0]

    conn = pymysql.connect(host="localhost", user="catch", password="1q2w3e4r!", db="catch", charset="utf8")
    curs = conn.cursor()

    current_time = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')

    sql = "UPDATE client_info SET access_time = %s WHERE id = %s"
    curs.execute(sql, (current_time, id))
    conn.commit()
    conn.close()

    client_socket.close()


def from_port_9502():  # CatchCatch 모듈로 부터 음성 데이터 전송을 위한 Socket 획득
    client_socket, addr = server_socket_9502.accept()
    t = threading.Thread(target=from_port_9502)
    t.start()

    recv_data = client_socket.recv(128)
    id = recv_data.decode().split(" ")[0]
    client_info_for_voice[id] = client_socket


def from_port_9600():  # CatchCatch 앱에 CatchCatch 모듈의 네트워크 상태 정보 제공
    client_socket, addr = server_socket_9600.accept()
    t = threading.Thread(target=from_port_9600)
    t.start()

    recv_data = client_socket.recv(128)
    id = recv_data.decode().split(" ")[0]

    conn = pymysql.connect(host="localhost", user="catch", password="1q2w3e4r!", db="catch", charset="utf8")
    curs = conn.cursor()

    sql = "SELECT access_time FROM client_info WHERE id=%s"
    curs.execute(sql, id)

    access_time = curs.fetchall()[0][0]
    access_time = datetime.datetime.strptime(access_time, '%Y-%m-%d %H:%M:%S')
    now_time = datetime.datetime.now()
    elapsed_time = now_time - access_time
    elapsed_time_second = elapsed_time.days * 24 * 3600 + elapsed_time.seconds

    if elapsed_time_second < 600:  # 600초 이상 CatchCatch 모듈에서 서버에 접근하지 않았을 경우
        client_socket.send(b'1')
    else:  # 600초 이내에 CatchCatch 모듈에서 서버에 접근했을 경우
        client_socket.send(b'0')


def from_port_9601():  # CatchCatch 앱에서 받은 사용자의 명령을 CatchCatch 모듈에 전송
    try:
        mobile_socket, addr = server_socket_9601.accept()
        t = threading.Thread(target=from_port_9601)
        t.start()

        mobile_recv_data = mobile_socket.recv(128).decode().split(" ")
        print("test", mobile_recv_data)  # test

        id = mobile_recv_data[0]
        catecory = mobile_recv_data[1]

        replaced_id = id.replace("@", "_").replace(".", "_")

        if catecory == "special_ability":
            mobile_socket.close()

            number = mobile_recv_data[2]

            send_data = "{} {} ".format(catecory, number)
            client_info[id].send(send_data.encode())

        elif catecory == "mode":
            mobile_socket.close()

            number = mobile_recv_data[2]

            send_data = "{} {} ".format(catecory, number)
            client_info[id].send(send_data.encode())

        elif catecory == "movement":
            mobile_socket.close()

            angle = mobile_recv_data[2]
            strength = mobile_recv_data[3]

            send_data = "{} {} {} ".format(catecory, angle, strength)
            client_info[id].send(send_data.encode())

        elif catecory == "keypad":
            mobile_socket.close()

            number = mobile_recv_data[2]

            send_data = "{} {} ".format(catecory, number)
            client_info[id].send(send_data.encode())

        elif catecory == "game_run":
            mobile_socket.close()

            number = mobile_recv_data[2]
            send_data = "{} {} ".format(catecory, number)
            client_info[id].send(send_data.encode())

        elif catecory == "game_stop":
            mobile_socket.close()

            number = mobile_recv_data[2]
            send_data = "{} {} ".format(catecory, number)
            client_info[id].send(send_data.encode())

        elif catecory == "streaming_recording":
            send_data = "{} ".format(catecory)
            client_info[id].send(send_data.encode())

            if client_info[id].recv(1) == b'1':
                mobile_socket.send(b'1')
            else:
                mobile_socket.send(b'0')

            mobile_socket.close()

        elif catecory == "streaming_downloading":
            send_data = "{} ".format(catecory)
            client_info[id].send(send_data.encode())

            if client_info[id].recv(1) == b'1':
                os.system("mv -f /usr/local/nginx/html/stream/{}_streaming.flv "
                          "/usr/local/nginx/html/stream/{}_streaming_back.flv".format(replaced_id, replaced_id))
                time.sleep(1)
                mobile_socket.send(b'1')
            else:
                mobile_socket.send(b'0')

            mobile_socket.close()

        elif catecory == "calibration":
            x0, y0, x1, y1 = mobile_recv_data[2], mobile_recv_data[3], mobile_recv_data[4], mobile_recv_data[5]
            send_data = "{} {} {} {} {} ".format(catecory, x0, y0, x1, y1)
            client_info[id].send(send_data.encode())

            if client_info[id].recv(1) == b'1':
                mobile_socket.send(b'1')
            else:
                mobile_socket.send(b'0')

            mobile_socket.close()

        elif catecory == "reset":
            send_data = "{} ".format(catecory)
            client_info[id].send(send_data.encode())

            if client_info[id].recv(1) == b'1':
                mobile_socket.send(b'1')
            else:
                mobile_socket.send(b'0')

            mobile_socket.close()

        elif catecory == "setting":
            max = mobile_recv_data[2]
            send_data = "{} {} ".format(catecory, max)
            client_info[id].send(send_data.encode())

            if client_info[id].recv(1) == b'1':
                mobile_socket.send(b'1')
            else:
                mobile_socket.send(b'0')

            mobile_socket.close()

    except KeyError as e:
        print("from_port_9601 error: ", e)


def from_port_9602():  # 음성 데이터를 CatchCatch 모듈에 전송
    mobile_socket, addr = server_socket_9602.accept()
    t = threading.Thread(target=from_port_9602)
    t.start()

    mobile_recv_data = mobile_socket.recv(128).decode().split(" ")
    id = mobile_recv_data[0]

    try:
        client_info_for_voice[id].send(mobile_recv_data[0].encode())
        mobile_socket.send(b'1')
    except KeyError as e:
        print("from_port_9602 error: ", e)
        mobile_socket.send(b'0')

    while True:
        mobile_recv_data = mobile_socket.recv(1024)
        if mobile_recv_data != b'':
            try:
                client_info_for_voice[id].send(mobile_recv_data)
            except KeyError as e:
                print("from_port_9602 error: ", e)
                break

            if mobile_recv_data[-10:] == b'CatchCatch':
                break
            elif mobile_recv_data[-10:] == b'ErrorError':
                break

    mobile_socket.close()


if __name__ == '__main__':
    t1 = threading.Thread(target=from_port_9500).start()
    t2 = threading.Thread(target=from_port_9501).start()
    t3 = threading.Thread(target=from_port_9502).start()
    t4 = threading.Thread(target=from_port_9600).start()
    t5 = threading.Thread(target=from_port_9601).start()
    t6 = threading.Thread(target=from_port_9602).start()
