#!/usr/bin/evn python
import pygame
import asyncio
import random
import math
import pyautogui
import socket
import os
import glob

live_list = [True, False, False, False]
WHITE = (255, 255, 255)
BLACK = (0, 0, 0)
pad_width = 1280  # 1024, 512   640, 360
pad_height = 720
mouse_width = 49  # 64, 64
mouse_height = 114
pang_width = 220
pang_height = 220
dirpath = os.path.dirname(os.path.realpath(__file__))
gamepad = pygame.display.set_mode((pad_width, pad_height))
socket_switch = False
control_angle = 0

X0 = 0
Y0 = 0
X1 = 0
Y1 = 0

socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# 통신 ------------------------------
if socket_switch:
    socket.connect(("127.0.0.1", 9505))


# 쥐가 보고있는 방향에 고양이가 있는지 판별하는 함수
def isCatDirection(mx, my, lx, ly, x1, y1, x2, y2):
    global cat_x, cat_y
    print("전해받은 고양이 좌표", x1, y1, x2, y2)
    # 세로 직선
    if lx - mx == 0:
        if x1 <= lx and lx <= x2:
            if ((my < ly) and (my > ((y1 + y2) / 2))) or ((my > ly) and (my < ((y1 + y2) / 2))):  # 고양이가 쥐 뒷통수에 있을 때
                return False

            return True
    # 가로 직선
    elif ly - my == 0:
        if y1 <= ly and ly <= y2:
            if ((mx < lx) and (mx > ((x1 + x2) / 2))) or ((mx > lx) and (mx < ((x1 + x2) / 2))):  # 고양이가 쥐 뒷통수에 있을 때
                return False

            return True
    else:
        if (x2 - x1) == 0:  # 기울기 분모 0일 때
            pass
        else:  # 나머지
            a = (ly - my) / (lx - mx)  # 기울기
            d = my - (a * mx)  # y절편
            a2 = (y2 - y1) / (x2 - x1)  # 상자 대각선 기울기

            if (a < a2) and (a > (-a2)):  # 가로 방향 체크
                if ((y1 <= (a * x1 + d)) and ((a * x1 + d) <= y2)) or ((y1 <= (a * x2 + d)) and ((a * x2 + d) <= y2)):
                    if ((mx < lx) and (mx > ((x1 + x2) / 2))) or (
                            (mx > lx) and (mx < ((x1 + x2) / 2))):  # 고양이가 쥐 뒷통수에 있을 때
                        return False

                    return True
            else:  # 세로 방향 체크
                if ((x1 <= ((y1 - d) / a)) and (((y1 - d) / a) <= x2)) or (
                        (x1 <= ((y2 - d) / a)) and (((y2 - d) / a) <= x2)):
                    if ((my < ly) and (my > ((y1 + y2) / 2))) or (
                            (my > ly) and (my < ((y1 + y2) / 2))):  # 고양이가 쥐 뒷통수에 있을 때
                        return False

                    return True

    # 안전한 방향
    return False


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

    def __init__(self, live, idx):
        # 쥐 각자의 속도
        self.live = live
        self.idx = idx
        live_list[idx] = live
        self.mspeed = self.default_mspeed
        # 쥐 위치 랜덤으로 정함
        self.x, self.y = random_destination(pad_width - mouse_width, pad_height - mouse_height)
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
        self.default_ani_speed = 1
        self.ani_speed = self.default_ani_speed
        self.imagelist = ratimagelist
        self.deading = False

        self._pos = None

    def update(self):
        # 쥐 이동

        if self.live:

            a = self.leader_x - self.x  # 이동 거리
            b = self.leader_y - self.y
            c = math.pow(math.pow(a, 2) + math.pow(b, 2), -0.5)
            dx = a * c * self.mspeed
            dy = b * c * self.mspeed
            self.x += dx  # 이동
            self.y += dy

            # 쥐 회전
            if self.is_rotating == True:  # 돌고있는 매 순간들
                if self.absolute_angle_sum >= self.mouse_angle - 10 and self.absolute_angle_sum <= self.mouse_angle + 10:  # 목표 방향 도착
                    self.is_rotating = False
                else:
                    self.angle_sum = self.angle_sum + 8 * self.rotation_direction  # 회전속도(8)
            elif self.is_rotating == False:  # 한번만 반짝 하는거
                self.is_rotating = True
                self.mouse_angle = math.degrees(
                    math.atan2(a, b) + math.pi)  # (self.leader_x - self.x, self.leader_y - self.y) -> (a, b)
                self.rotation_direction = direction_decision(self.absolute_angle_sum, self.mouse_angle)

            self.absolute_angle_sum = abs(self.angle_sum % 360)
            # 애니메이션 이미지 회전
            self.image = self.imagelist[self.ani_pos]
            self.ani_speed -= 1

            if self.ani_speed <= 0:
                self.ani_speed = self.default_ani_speed
                if self.ani_pos == self.ani_max:
                    self.ani_pos = 0
                else:
                    self.ani_pos += 1

            self.rotation_mouse = pygame.transform.rotate(self.image, self.angle_sum)

            self._pos = self.rotation_mouse.get_rect()
            self._pos.center = (self.x, self.y)
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
                    gamepad.blit(self.rotation_mouse, self._pos)

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

            if self.leader_x - (self.mspeed / 2) <= self.x and self.leader_x + (self.mspeed / 2) >= \
                    self.x and self.leader_y - (self.mspeed / 2) <= self.y and self.leader_y + (
                    self.mspeed / 2) >= self.y:
                self.leader_x, self.leader_y = random_destination(pad_width, pad_height)
            if isCatDirection(self.x, self.y, self.leader_x, self.leader_y, X0, Y0, X1, Y1):
                self.leader_x, self.leader_y = random_destination(pad_width, pad_height)

# 컨트롤 쥐 클래스
class ControlMouse():
    default_mspeed = 10
    live = False
    def __init__(self, live):
        # 쥐 각자의 속도
        self.live = live
        self.mspeed = self.default_mspeed
        # 쥐 위치 랜덤으로 정함
        self.x, self.y = random_destination(pad_width - mouse_width, pad_height - mouse_height)
        self.is_rotating = False
        self.angle_sum = 0
        self.mouse_angle = 0
        self.absolute_angle_sum = 0
        # rotation_direction = 1  # 쥐 회전 방향 (1 : 우회전, -1: 좌회전)
        self.rotation_direction = 1
        self.rotation_mouse = None  # 0 -> None
        # 쥐의 이동 방향
        self.leader_x, self.leader_y = self.x+100, self.y+100
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
        self.default_ani_speed = 1
        self.ani_speed = self.default_ani_speed
        self.imagelist = ratimagelist
        self.deading = False

        self._pos = None


    def update(self):
        # 쥐 이동

        if self.live:

            a = self.leader_x - self.x  # 이동 거리
            b = self.leader_y - self.y
            c = math.pow(math.pow(a, 2) + math.pow(b, 2), -0.5)
            dx = a * c * self.mspeed
            dy = b * c * self.mspeed
            self.x += dx  # 이동
            self.y += dy

            # 쥐 회전
            if self.is_rotating == True:  # 돌고있는 매 순간들
                if self.absolute_angle_sum >= self.mouse_angle - 10 and self.absolute_angle_sum <= self.mouse_angle + 10:  # 목표 방향 도착
                    self.is_rotating = False
                else:
                    self.angle_sum = self.angle_sum + 8 * self.rotation_direction  # 회전속도(8)
            elif self.is_rotating == False:  # 한번만 반짝 하는거
                self.is_rotating = True
                self.mouse_angle = math.degrees(
                    math.atan2(a, b) + math.pi)  # (self.leader_x - self.x, self.leader_y - self.y) -> (a, b)
                self.rotation_direction = direction_decision(self.absolute_angle_sum, self.mouse_angle)

            self.absolute_angle_sum = abs(self.angle_sum % 360)
            # 애니메이션 이미지 회전
            self.image = self.imagelist[self.ani_pos]
            self.ani_speed -= 1

            if self.ani_speed <= 0:
                self.ani_speed = self.default_ani_speed
                if self.ani_pos == self.ani_max:
                    self.ani_pos = 0
                else:
                    self.ani_pos += 1

            self.rotation_mouse = pygame.transform.rotate(self.image, self.angle_sum)

            self._pos = self.rotation_mouse.get_rect()
            self._pos.center = (self.x, self.y)
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
                    gamepad.blit(self.rotation_mouse, self._pos)

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

                gamepad.blit(self.pang, self.pang_pos)


            # 각도 계산 시작
            g = math.tan((-math.pi / 180) * control_angle)
            b = self.y - g * self.x
            nx = self.x + 100
            ny = g * nx + b
            a = nx - self.x
            b = ny - self.y
            # leader 위치
            movex = 100 * a * math.pow(math.pow(a, 2) + math.pow(b, 2), -0.5)
            movey = 100 * b * math.pow(math.pow(a, 2) + math.pow(b, 2), -0.5)
            if control_angle > 90 and control_angle <= 270:
                self.leader_x, self.leader_y = self.x - movex, self.y - movey
            else:
                self.leader_x, self.leader_y = self.x + movex, self.y + movey





# while문 반복
async def run_Game():
    global cat_x, cat_y, live_list
    global X0, Y0, X1, Y1
    global meat_punch_sound
    global control_angle
    pyautogui.moveTo(600, 1000)  # 마우스 포인터 우측 하단으로 치우기
    cat_x = -500
    cat_y = -500
    cat_xchange = 0
    cat_ychange = 0
    mouse_speed = 10  # 12
    meat_punch_sound = pygame.mixer.Sound(dirpath + '/sound/Meat_Punch01.wav')

    mouse_list = [Mouse(True, 0), Mouse(False, 1), Mouse(False, 2), Mouse(False, 3)]
    controlMouse = ControlMouse(True);
    crashed = False

    while not crashed:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                crashed = True
            if event.type == pygame.KEYDOWN:
                if event.key == pygame.K_UP:
                    cat_ychange = -5
                elif event.key == pygame.K_DOWN:
                    cat_ychange = 5
                elif event.key == pygame.K_LEFT:
                    cat_xchange = -5
                elif event.key == pygame.K_RIGHT:
                    cat_xchange = 5
                elif event.key == pygame.K_LCTRL:  # 쥐 네마리까지
                    if False in live_list:
                        idx = live_list.index(False)
                        mouse_list[idx].__init__(True, idx)


                elif event.key == pygame.K_LSHIFT:
                    # MouseCount -= 1
                    pass

                elif event.key == pygame.K_w:
                    control_angle = 90
                elif event.key == pygame.K_s:
                    control_angle = 270
                elif event.key == pygame.K_a:
                    control_angle = 180
                elif event.key == pygame.K_d:
                    control_angle = 0


                elif event.key == pygame.K_z:
                    controlMouse.__init__(True)

                    '''
                    mouse = pygame.image.load('images/mouse.png')
                    mouse = pygame.transform.scale(mouse, (mouse_width, mouse_height))
                    '''
                elif event.key == pygame.K_x:
                    controlMouse.live = False
                    '''
                    mouse = pygame.image.load('images/mouse_black.png')
                    mouse = pygame.transform.scale(mouse, (mouse_width, mouse_height))
                    '''

            if event.type == pygame.KEYUP:
                if event.key == pygame.K_UP:
                    cat_ychange = 0
                elif event.key == pygame.K_DOWN:
                    cat_ychange = 0
                elif event.key == pygame.K_LEFT:
                    cat_xchange = 0
                elif event.key == pygame.K_RIGHT:
                    cat_xchange = 0
                elif event.key == pygame.K_ESCAPE:
                    crashed = True

        # 매 프레임마다 이전프레임의 잔상 제거
        gamepad.fill(WHITE)
        # instantiate(grass, 0, 0)

        send_data = "receive_cat_info"
        # 통신 ----------------------------------------------------------
        if socket_switch:
            socket.send(send_data.encode())
            recv_data = socket.recv(32).decode("UTF-8")
            data = recv_data.split(" ")
            X0, Y0, X1, Y1 = int(data[0]), int(data[1]), int(data[2]), int(data[3])
            print("받음 :", X0, Y0, X1, Y1)
        else:
            X0, Y0, X1, Y1 = cat_x, cat_y, cat_x + 50, cat_y + 50
        # 고양이 이동
        cat_x += cat_xchange
        cat_y += cat_ychange

        ncat = pygame.transform.scale(cat, (X1 - X0, Y1 - Y0))
        instantiate(ncat, X0, Y0)
        for x in mouse_list:
            x.update()
        controlMouse.update()

        print("살아있는 쥐 :", live_list)
        # 마지막
        pygame.display.update()
        clock.tick(60)  # 프레임 수
    pygame.quit()
    quit()


def init_Game():
    global gamepad, clock, grass
    global mouse, cat, ratpang
    global ratani, ratimagelist
    global ratpang, ratpanglist

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
    ratani = glob.glob(dirpath + '/images/rat/Armature_move_*.png')
    ratani.sort()
    ratimagelist = []
    for i in range(len(ratani)):
        ratimagelist.append(pygame.transform.scale(pygame.image.load(ratani[i]), (mouse_width, mouse_height)))

    # 고양이
    # cat = pygame.image.load('/home/full/CatchCatch/images/cat.png')
    cat = pygame.image.load('images/cat.png')
    cat = pygame.transform.scale(cat, (128, 128))
    # 배경
    # grass = pygame.image.load('/home/full/CatchCatch/images/grass.png')
    grass = pygame.image.load('images/grass.png')
    grass = pygame.transform.scale(grass, (pad_width, pad_height))

    # 시작
    loop = asyncio.get_event_loop()
    loop.run_until_complete(run_Game())
    loop.close()


init_Game()
