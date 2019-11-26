import socket
import time
import os
import threading
import subprocess
from playsound import playsound
import json
import pyautogui

path = os.path.dirname(os.path.abspath(__file__))

with open("{}/../../conf.json".format(path)) as json_read_file:
    json_data = json.load(json_read_file)
json_read_file.close()

id = json_data["id"]
replaced_id = id.replace("@", "_").replace(".", "_")

def to_port_9500():  # CatchCatch 앱으로부터 오는 명령을 서버로 부터 받기 위해 서버에 연결
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(('15.164.75.141', 9500))
        sock.send(id.encode())

        sock2 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock2.connect(('127.0.0.1', 9505))

        os.system("ffmpeg -f v4l2 -hide_banner -threads 4 "
                  "-framerate 18 "
                  "-video_size 640x360 "
                  "-input_format mjpeg "
                  "-i /dev/video0 "
                  "-vcodec libx264 "
                  "-crf 22 "
                  "-profile:v baseline "
                  "-level 3.0 "
                  "-movflags +faststart "
                  "-pix_fmt yuv420p "
                  "-f flv rtmp://15.164.75.141/hls/{}_streaming > /dev/null 2>&1 &".format(replaced_id))

        while True:
            recv_data = sock.recv(128)
            recv_data = recv_data.decode().split(" ")

            if recv_data[0] == '':
                pass
            else:
                # print("test", recv_data)  # test
                
                category = recv_data[0]

                if category == "special_ability":
                    number = recv_data[1]

                    send_data = "send_special_ability_info {} ".format(number)

                    sock2.send(send_data.encode())

                if category == "mode":
                    number = recv_data[1]

                    send_data = "send_mode_info {} ".format(number)

                    sock2.send(send_data.encode())

                elif category == "movement":
                    angle = recv_data[1]
                    strength = recv_data[2]

                    send_data = "send_movement_info {} {} ".format(angle, strength)

                    sock2.send(send_data.encode())

                elif category == "keypad":
                    number = recv_data[1]

                    send_data = "send_keypad_info {} ".format(number)

                    sock2.send(send_data.encode())

                elif category == "game_run":
                    pyautogui.press("esc")

                    number = recv_data[1]

                    if number == "0":  # Catch Rat
                        os.system("dbus-launch gnome-terminal -- python3 {}/../../game/MouseGame.py".format(path))
                    elif number == "1":  # Catch Fish
                        os.system("dbus-launch gnome-terminal -- python3 {}/../../game/FishGame.py".format(path))
                    elif number == "2":  # Catch Mole
                        os.system("dbus-launch gnome-terminal -- python3 {}/../../game/MoleGame.py".format(path))

                elif category == "game_stop":
                    pyautogui.press("esc")

                elif category == "streaming_recording":
                    # ffmpeg 종료
                    while True:
                        os.system("ps -ef | "
                                  "grep 'ffmpeg -f v4l2' | "
                                  "grep -v grep | "
                                  "awk '{print $2}' > " +
                                  "{}/bg_ffmpeg".format(path))

                        f = open("{}/bg_ffmpeg".format(path), "r")
                        process_num = f.readline()
                        if process_num == "":
                            break
                        os.system("kill {}".format(process_num))
                        f.close()

                    time.sleep(1)
                    os.system("ffmpeg -f v4l2 -hide_banner -threads 4 "
                              "-framerate 18 "
                              "-video_size 640x360 "
                              "-input_format mjpeg "
                              "-i /dev/video0 "
                              "-vcodec libx264 "
                              "-crf 22 "
                              "-profile:v baseline "
                              "-level 3.0 "
                              "-movflags +faststart "
                              "-pix_fmt yuv420p "
                              "-f flv rtmp://15.164.75.141/hls/{}_streaming > /dev/null 2>&1 &".format(replaced_id))
                    sock.send(b'1')

                elif category == "streaming_downloading":
                    time.sleep(5)
                    # ffmpeg 종료
                    while True:
                        os.system("ps -ef | "
                                  "grep 'ffmpeg -f v4l2' | "
                                  "grep -v grep | "
                                  "awk '{print $2}' > " +
                                  "{}/bg_ffmpeg".format(path))

                        f = open("{}/bg_ffmpeg".format(path), "r")
                        process_num = f.readline()
                        if process_num == "":
                            break
                        os.system("kill {}".format(process_num))
                        f.close()

                    sock.send(b'1')
                    time.sleep(1)
                    os.system("ffmpeg -f v4l2 -hide_banner -threads 4 "
                              "-framerate 18 "
                              "-video_size 640x360 "
                              "-input_format mjpeg "
                              "-i /dev/video0 "
                              "-vcodec libx264 "
                              "-crf 22 "
                              "-profile:v baseline "
                              "-level 3.0 "
                              "-movflags +faststart "
                              "-pix_fmt yuv420p "
                              "-f flv rtmp://15.164.75.141/hls/{}_streaming > /dev/null 2>&1 &".format(replaced_id))

                elif category == "calibration":
                    sock.send(b'1')
                    x0, y0, x1, y1 = recv_data[1], recv_data[2], recv_data[3], recv_data[4]
                    send_data = "send_calibration_info {} {} {} {} ".format(x0, y0, x1, y1)
                    sock2.send(send_data.encode())

                elif category == "reset":
                    sock.send(b'1')
                    os.system("sudo python3 {}/../../reset.py".format(path))

                elif category == "setting":
                    sock.send(b'1')
                    max = recv_data[1]
                    send_data = "send_setting_info {} ".format(max)
                    sock2.send(send_data.encode())

    except Exception as e:  # 에러 발생시 다시 to_port_9500 실행
        print("to_port_9500 error: ", e)
        time.sleep(1)
        to_port_9500()


def to_port_9501():  # CatchCatch 모듈의 네트워크 상태가 정상인 것을 알리기 위해 2분 30초 마다 서버에 연결
    try:
        while True:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.connect(('15.164.75.141', 9501))
            sock.send(id.encode())
            sock.close()
            time.sleep(150)

    except Exception as e:  # 에러 발생시 다시 to_port_9501 실행
        print("to_port_9501 error: ", e)
        time.sleep(1)
        to_port_9501()


def to_port_9502():  # 음성 데이터를 서버로 부터 받기 위해 서버에 연결
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(('15.164.75.141', 9502))
        sock.send(id.encode())

        while True:
            recv_data = sock.recv(1024)

            if recv_data == b'':
                pass
            else:
                if recv_data == id.encode():
                    f = open("{}/voice.mp3".format(path), "wb")

                    while True:
                        recv_data = sock.recv(1024)

                        if recv_data == b'':
                            pass
                        else:
                            if recv_data[-10:] == b'CatchCatch':
                                f.write(recv_data[:-10])
                                f.close()

                                threading.Thread(target=voice_on).start()
                                break
                            elif recv_data[-10:] == b'ErrorError':
                                f.close()

                                break
                            else:
                                f.write(recv_data)

    except Exception as e:  # 에러 발생시 다시 to_port_9500 실행
        print("to_port_9502 error: ", e)
        time.sleep(1)
        to_port_9502()


def voice_on():
    playsound("{}/voice.mp3".format(path))


if __name__ == '__main__':
    # 초기 설정 확인
    initial_setting = json_data["initial_setting"]

    if initial_setting == "true":
        t1 = threading.Thread(target=to_port_9500).start()
        t2 = threading.Thread(target=to_port_9501).start()
        t3 = threading.Thread(target=to_port_9502).start()
