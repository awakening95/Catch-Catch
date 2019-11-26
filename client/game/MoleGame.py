#!/usr/bin/evn python
import pygame
import asyncio
import random
import math
import pyautogui
import socket
import os
import glob

live_list = [True, True, True, True, True, True, True, True, True]
WHITE = (255, 255, 255)
BLACK = (0, 0, 0)
pad_width = 1280  # 1024, 512   640, 360
pad_height = 720
mouse_width = 100  # 64, 64
mouse_height = 114
pang_width = 220
pang_height = 220
dirpath = os.path.dirname(os.path.realpath(__file__))
gamepad = pygame.display.set_mode((pad_width, pad_height), pygame.FULLSCREEN)  # , pygame.FULLSCREEN
socket_switch = True

X0 = 0
Y0 = 0
X1 = 0
Y1 = 0

socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# 통신 ------------------------------
if socket_switch:
    socket.connect(("127.0.0.1", 9505))



def direction_decision(my_direction, goal_direction):
    if my_direction <= 180:  # 쥐가 180 이하인가
        if goal_direction <= my_direction:  # 90(목표 각도. 90은 임의 값)보다 작은가
            return -1  # 우회전
        else:  # 90보다 작지 않다
            if goal_direction <= (my_direction + 180):  # 90+180보다 작은가
                return 1  # 좌회전
            else:
                return -1  # 우회전
    else:  # 쥐가 180 이하가 아니다. 180보다 크다.
        if goal_direction > my_direction:  # 270(임의 값)보다 큰가
            return 1  # 좌회전
        else:  # 270보다 작다
            if goal_direction > (my_direction - 180):  # 270-180보다 큰가
                return -1  # 우회전
            else:
                return 1  # 좌회전


# 화면에 객체 그리기
def instantiate(object, x, y):
    gamepad.blit(object, (x, y))


# 랜덤 좌표 생성 함수
def random_destination(x, y):
    x = random.randint(0, x)
    y = random.randint(0, y)
    return [x, y]


# 쥐 클래스
class Mouse():
    global X0, X1, Y0, Y1, live_list
    default_mspeed = 10

    def __init__(self, live, idx, holePoint):
        # 쥐 각자의 속도
        self.live = live
        self.idx = idx
        live_list[idx] = live
        # 쥐 위치 고정
        self.x, self.y = holePoint[0], holePoint[1]  #random_destination(pad_width - mouse_width, pad_height - mouse_height)
        self.is_rotating = False
        self.angle_sum = 0
        self.mouse_angle = 0
        self.absolute_angle_sum = 0
        # rotation_direction = 1  # 쥐 회전 방향 (1 : 우회전, -1: 좌회전)
        self.rotation_direction = 1
        self.rotation_mouse = None  # 0 -> None
        # 쥐의 이동 방향
        self.leader_x, self.leader_y = random_destination(pad_width - mouse_width, pad_height - mouse_height)
        # 죽은 위치
        self.pang_point = None
        # 터지는 효과 유지 프레임

        self.panglist = ratpanglist
        self.pang_max = len(ratpang)
        self.default_pang_speed = 2
        self.pang_speed = self.default_pang_speed
        self.pang_ani_pos = None

        self.ani_pos = 0  # 현재 띄우고 있는 애니메이션 이미지 번호
        self.ani_max = len(ratani) - 1
        self.default_ani_speed = 0
        self.booster_ani_speed = 0
        self.ani_speed = self.default_ani_speed
        self.imagelist = ratimagelist
        self.hole = hole
        self.deading = False

        self._pos = None

        self.default_count = 60
        self.count = self.default_count  # count 프레임에 한 번 랜덤 튀어나오기 시도
        self.appear = False

    def update(self):
        self._pos = self.hole.get_rect()
        self._pos.center = (self.x, self.y)
        if self.live:
            if control_auto:
                if not self.appear:
                    self.count -= 1

                if self.count <= 0:
                    self.count = self.default_count
                    if random.randint(0, 10) == 1:
                        self.appear = True
            else:
                if control_location == self.idx:
                    self.appear = True


            # 애니메이션 이미지
            if self.appear:
                self.image = self.imagelist[self.ani_pos]
                self.ani_speed -= 1

                if self.ani_speed <= 0:
                    if control_booster:
                        self.ani_speed = self.booster_ani_speed
                    else:
                        self.ani_speed = self.default_ani_speed
                    if self.ani_pos == self.ani_max:
                        self.ani_pos = 0
                    else:
                        self.ani_pos += 1





                if not self.deading:
                    if self.x >= X0 and self.x <= X1 and self.y >= Y0 and self.y <= Y1:  # 쥐가 잡히면
                        self.deading = True
                        # 소리
                        meat_punch_sound.play()
                        # 잡힌 위치 추가, 프레임 카운트 시작
                        self.pang_point = [self.x, self.y]
                        self.pang_ani_pos = 0
                        self.mspeed = 0
                    else:
                        gamepad.blit(self.image, self._pos)
                        if self.ani_pos == self.ani_max:
                            self.ani_pos = 0
                            self.appear = False

            else:
                gamepad.blit(self.hole, self._pos)  # 구멍 이미지



            if self.pang_ani_pos is not None:
                # ---팡
                self.pang_speed -= 1
                self.pang = self.panglist[self.pang_ani_pos]
                self.pang_pos = self.pang.get_rect()
                self.pang_pos.center = (self.pang_point[0], self.pang_point[1])
                if self.pang_speed <= 0:
                    self.pang_speed = self.default_pang_speed
                    self.pang_ani_pos += 1
                    if self.pang_ani_pos >= self.pang_max:
                        self.live = False
                        live_list[self.idx] = False
                gamepad.blit(self.pang, self.pang_pos)



class StageManager():
    def __init__(self, live_list):
        self.length = len(live_list)
        self.maxCount = 75
        self.count = 0

    def update(self, live_list, mouse_list):
        if (True not in live_list):
            self.count += 1
            if self.count >= self.maxCount:
                self.count = 0
                k = 0
                holeY = -160
                for i in range(3):
                    holeX = -440
                    holeY += 260
                    for j in range(3):
                        holeX += 540
                        mouse_list[k] = Mouse(True, k, [holeX, holeY])
                        k += 1



# while문 반복
async def run_Game():
    global cat_x, cat_y, live_list
    global X0, Y0, X1, Y1
    global meat_punch_sound
    global control_auto, control_location, control_booster
    global mouse_list
    pyautogui.moveTo(600, 1000)  # 마우스 포인터 우측 하단으로 치우기
    cat_x = -500
    cat_y = -500

    meat_punch_sound = pygame.mixer.Sound(dirpath + '/sound/Meat_Punch01.wav')
    control_booster = False
    mouse_list = [None] * 9
    k = 0
    holeY = -160
    for i in range(3):
        holeX = -440
        holeY += 260
        for j in range(3):
            holeX += 540
            mouse_list[k] = Mouse(True, k, [holeX, holeY])
            k += 1
    crashed = False
    control_location = None
    stageManager = StageManager(live_list)
    while not crashed:
        for event in pygame.event.get():
            if event.type == pygame.KEYUP:
                if event.key == pygame.K_ESCAPE:
                    crashed = True

        # 매 프레임마다 이전프레임의 잔상 제거
        instantiate(background, 0, 0)

        send_data1 = "receive_cat_info"
        send_data2 = "receive_keypad_info"
        # 통신 ----------------------------------------------------------
        if socket_switch:
            socket.send(send_data1.encode())
            recv_data = socket.recv(32).decode("UTF-8")
            data = recv_data.split(" ")
            X0, Y0, X1, Y1 = int(data[0]), int(data[1]), int(data[2]), int(data[3])

            socket.send(send_data2.encode())
            recv_data = socket.recv(32).decode("UTF-8")
            data = recv_data.split(" ")
            control_location, control_booster, control_auto = int(data[0]), data[1], data[2]  # 숫자 0~8, 불린, 불린

            if control_booster == 'true':
                control_booster = True
            else:
                control_booster = False
            if control_auto == 'auto':
                control_auto = True
            else:
                control_auto = False
        else:
            X0, Y0, X1, Y1 = cat_x, cat_y, cat_x + 50, cat_y + 50
            control_auto = True

        for x in mouse_list:
            x.update()

        if not socket_switch:
            control_location = None

        stageManager.update(live_list, mouse_list)
        # 마지막
        pygame.display.update()
        clock.tick(60)  # 프레임 수
    pygame.quit()
    quit()


def init_Game():
    global gamepad, clock, grass
    global mouse, cat, ratpang
    global ratani, ratimagelist, hole
    global ratpang, ratpanglist
    global background

    pygame.init()

    pygame.display.set_caption('Mouse Game')
    clock = pygame.time.Clock()

    # 폭발
    ratpang = glob.glob(dirpath + '/images/Explosion/explosion_*.png')
    ratpang.sort()
    ratpanglist = []
    for i in range(len(ratpang)):
        ratpanglist.append(pygame.transform.scale(pygame.image.load(ratpang[i]), (pang_width, pang_height)))

    # 쥐
    hole = dirpath + '/images/mole/hole.png'
    hole = pygame.transform.scale(pygame.image.load(hole), (mouse_width, mouse_height))
    ratani = glob.glob(dirpath + '/images/mole/appear_*.png')
    ratani.sort()
    ratimagelist = []
    for i in range(len(ratani)):
        ratimagelist.append(pygame.transform.scale(pygame.image.load(ratani[i]), (mouse_width, mouse_height)))

    # 고양이
    cat = pygame.image.load(dirpath +'/images/cat.png')
    cat = pygame.transform.scale(cat, (128, 128))
    # 배경
    background = pygame.image.load(dirpath + '/images/background/DirtGround.png')
    background = pygame.transform.scale(background, (pad_width, pad_height))

    # 시작
    loop = asyncio.get_event_loop()
    loop.run_until_complete(run_Game())
    loop.close()


init_Game()
