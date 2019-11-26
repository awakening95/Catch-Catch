# MobileSSD for Catch Catch



### Introduction

해당 프로그램은 MobileSSD의 코드를 수정하여 Catch Catch에 맞게 변형 되었습니다.



- 초반에 프로그램을 시작하면, 웹캠에서 영상이 나오고, 여기에서 빔 프로젝터의 네 꼭짓점을 선택합니다. 

- 해당 네 꼭짓점을 기준으로 Camera - Projector Calibration을 한 후, MobileSSD를 이용해 Calibration 된 좌표의 어디에 사물이 있는지 찾아냅니다.

- `cat`, `dog`, `sheep`, `horse`,`cow`라면 출력합니다. 



`cow`, `sheep`과 `horse`는 일반적인 반려 동물은 아니지만, 가끔 `cat`과 `dog`를 이로 인식할 때가 있어 정확도를 위해 추가하였습니다.

 현재 테스트를 위해 `person`도 판단하고 있지만 추후 제거할 예정입니다.



### Usage

```
python main.py ./data/my_data/dog1.mp4
```



