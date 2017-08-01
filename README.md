# sliding-puzzles
Flexible-sized auto-solver-enabled sliding puzzles game on android

# FAQs (also on game's FAQ activities)
```How to go to next page?``` <br />
There are menu items at top of screen except in main menu. It can help you navigate or hide/show items. Sometimes you can’t proceed and message will show up or UI will change.
<br />
<br />

```How to play this game?``` <br />
There 2 modes, random vs customized. Both require you set width and height and select image, though random gives you some predefined images. Customized also require you to select a position as blank and set start and goal positions correctly. The rule of game is at every move you should find where blank tile is and place an adjacent tile to it (swapping positions). This continued until you gets to the goal state. 
<br />
<br />

```What to do after auto-solve (i.e. after clicking ‘Solve it!’)?``` <br />
After auto-solve, you have no control over where to move the tiles. Indeed, every time you click on the board, a pre-calculated tile will be moved, and when you click on the background, the previous move will be undoed. In summary, click board will move forward one move and click elsewhere will move backward one move.
<br />
<br />

```Why sometimes calculating-moves/auto-solve stay 99% for a long time?``` <br />
Currently my algorithm is not deterministic, so I have no way to get the exact time in order to solve the puzzle. Instead, I have to estimate the required time by testing. However, you can start other tasks if you don’t want to wait, and it will calculate in background (as long as android is not too busy). 
<br />
<br />

```Why sometimes when I set start and goal it says it is impossible to solve?``` <br />
This due to the nature of game. Indeed, exactly half of the initial configuration is unsolvable (https://en.wikipedia.org/wiki/15_puzzle#Solvability). Simply swapping any 2 distinct non-blank symbol (in either start configuration or goal but not both) will resolve this issue.
