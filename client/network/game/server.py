import socket
import os
import threading
import json
import time

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(('127.0.0.1', 9505))
server_socket.listen(5)

path = os.path.dirname(os.path.abspath(__file__))

with open("{}/conf.json".format(path)) as json_read_file:
    json_data = json.load(json_read_file)
json_read_file.close()

x0, y0, x1, y1 = "0", "0", "0", "0"
converted_x0, converted_y0, converted_x1, converted_y1 = json_data["converted_x0"], json_data["converted_y0"], json_data["converted_x1"], json_data["converted_y1"]
angle, strength, number, special_ability, mode = "0", "0", "-1", "0", "auto"
max = json_data["max"]

if int(converted_x1) == int(converted_x0):
    ratio_x = 1.0
else:
    ratio_x = 1280.0 / (int(converted_x1) - int(converted_x0))

if int(converted_y1) == int(converted_y0):
    ratio_y = 1.0
else:
    ratio_y = 720.0 / (int(converted_y1) - int(converted_y0))


def net(client_socket, lock):
    global x0, y0, x1, y1
    global converted_x0, converted_y0, converted_x1, converted_y1
    global angle, strength, number, special_ability, mode, max
    global ratio_x, ratio_y

    while True:
        try:
            recv_data = client_socket.recv(128).decode('UTF-8')

            if recv_data == "":
                client_socket.close()
                break
            else:
                data = recv_data.split(" ")
                # print(data)

                if data[0] == "send_cat_info":
                    lock.acquire()
                    
                    if data[1] != "-1" or data[2] != "-1" or data[3] != "-1" or data[4] != "-1":
                        x0, y0, x1, y1 = str(int((int(data[1]) - int(converted_x0)) * ratio_x)), \
                                         str(int((int(data[2]) - int(converted_y0)) * ratio_y)), \
                                         str(int((int(data[3]) - int(converted_x0)) * ratio_x)), \
                                         str(int((int(data[4]) - int(converted_y0)) * ratio_y))

                        if int(x0) < 0:
                            x0 = "0"

                        if int(y0) < 0:
                            y0 = "0"

                        if int(x1) > 1280:
                            x1 = "1280"

                        if int(y1) > 720:
                            y1 = "720"

                    else:
                        x0, y0, x1, y1 = "-1", "-1", "-1", "-1"

                    # print("send_cat_info", x0, y0, x1, y1)  # test
                    lock.release()

                elif data[0] == "receive_cat_info":
                    lock.acquire()
                    data = x0 + " " + y0 + " " + x1 + " " + y1
                    # print("receive_cat_info", data)  # test
                    lock.release()
                    client_socket.send(data.encode())
                    x0, y0, x1, y1 = "-1", "-1", "-1", "-1"

                elif data[0] == "send_special_ability_info":
                    lock.acquire()
                    if data[1] == "1":
                        special_ability = "1"
                    else:
                        special_ability = "0"
                    lock.release()

                elif data[0] == "send_mode_info":
                    lock.acquire()
                    if data[1] == "1":
                        mode = "manual"
                    else:
                        mode = "auto"
                    lock.release()

                elif data[0] == "send_movement_info":
                    lock.acquire()
                    angle, strength = data[1], data[2]
                    lock.release()

                elif data[0] == "receive_joystick_info":
                    lock.acquire()
                    data = angle + " " + strength + " " + special_ability + " " + mode + " " + max
                    lock.release()
                    client_socket.send(data.encode())

                elif data[0] == "send_keypad_info":
                    lock.acquire()
                    number = data[1]
                    lock.release()

                elif data[0] == "receive_keypad_info":
                    lock.acquire()
                    data = number + " " + special_ability + " " + mode
                    lock.release()
                    client_socket.send(data.encode())
                    number = "-1"

                elif data[0] == "send_calibration_info":
                    lock.acquire()
                    converted_x0, converted_y0, converted_x1, converted_y1 = data[1], data[2], data[3], data[4]

                    if int(converted_x1) == int(converted_x0):
                        ratio_x = 1.0
                    else:
                        ratio_x = 1280.0 / (int(converted_x1) - int(converted_x0))

                    if int(converted_y1) == int(converted_y0):
                        ratio_y = 1.0
                    else:
                        ratio_y = 720.0 / (int(converted_y1) - int(converted_y0))

                    # print("send_calibration_info", converted_x0, converted_y0, converted_x1, converted_y1, ratio_x, ratio_y)  # test
                    lock.release()

                    json_data["converted_x0"], json_data["converted_y0"], json_data["converted_x1"], json_data["converted_y1"] \
                        = converted_x0, converted_y0, converted_x1, converted_y1

                    with open("{}/conf.json".format(path), 'w') as json_write_file:
                        json.dump(json_data, json_write_file)
                    json_write_file.close()

                elif data[0] == "send_setting_info":
                    lock.acquire()
                    max = data[1]
                    lock.release()

                    json_data["max"] = max

                    with open("{}/conf.json".format(path), 'w') as json_write_file:
                        json.dump(json_data, json_write_file)
                    json_write_file.close()

        except Exception as e:  # 에러 발생시 다시 to_port_9500 실행
            print("game server error: ", e)
            time.sleep(1)


if __name__ == '__main__':
    # 초기 설정 확인
    with open("{}/../../conf.json".format(path)) as json_read_file:
        json_data2 = json.load(json_read_file)
    json_read_file.close()

    initial_setting = json_data2["initial_setting"]

    if initial_setting == "true":
        lock = threading.Lock()

        while True:
            client_socket, addr = server_socket.accept()
            t = threading.Thread(target=net, args=(client_socket, lock))
            t.start()
