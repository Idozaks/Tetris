import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class tetris extends PApplet {

public Tile[][] board = new Tile[21][12]; //<>//
int boardCols = board[0].length, boardRows = board.length;
int tileWidth = 30;
Shape shape, ghost;
public int gray = 0xff747474, ghostGlow=0xffadadad;
char[] shapes = {'i', 'j', 'l', 'o', 's', 't', 'z'};
boolean downHeld = false;
boolean helperText=false;
boolean running = false;
boolean pausedText=false;
boolean lineFlash=false;
int timer = 0;
int speed = 0;
final int normalSpeed = 800;
final int fastSpeed = 30;
boolean started = false;

float horizSpacer; 
float vertSpacer;
int boardWidth = boardCols * tileWidth;
int boardHeight = boardRows * tileWidth;

ArrayList<ArrayList<Tile>> segments = new ArrayList();

public void setup() {
  //size(800, 1000);
  
  for (int i = 0; i < 21; i++) {
    for (int j = 0; j < 12; j++) {
      board[i][j] = new Tile(gray, i, j, false);
    }
  }
}

public void draw() {
  background(0);
  textSize(8);
  horizSpacer =(width-tileWidth*boardCols)/2;
  vertSpacer = (height-tileWidth*boardRows)/2;
  for (int i = 0; i < boardRows; i++) {
    for (int j = 0; j < boardCols; j++) {
      if (running)  if (board[i][j].Empty()) {
        if (board[i][j].colour == ghostGlow) {
          board[i][j].colour = gray;
        }
      }
    }
  }
  if (running && shape!=null) {
    showGhost();
  }
  for (int i = 0; i < boardRows; i++) {
    for (int j = 0; j < boardCols; j++) {
      fill(board[i][j].getColor());
      rect(j*tileWidth+horizSpacer, i*tileWidth+vertSpacer, tileWidth, tileWidth);
      if (helperText) {
        fill(0);
        text(""+i+","+j, j*tileWidth+horizSpacer+tileWidth/6, i*tileWidth+vertSpacer+tileWidth/2);
      }
    }
  }
  if (running) {
    speed = (downHeld)? fastSpeed: normalSpeed;
    if (millis() - timer >= speed) {
      if (shape!=null && running) {
        if (!shape.Fall()) {
          shape = null;
          clearLines();
          spawnShape();
        }
      }
      timer = millis();
    }
  }

  if (pausedText) {
    //add cool color effect on text? or maybe breating size effect
    pushStyle();
    textSize(72);
    fill(255);
    textAlign(CENTER);
    PFont font = loadFont("Assistant-ExtraBold-72.vlw");
    textFont(font);
    text("PAUSED", width/2, height/2);
    popStyle();
  }

  if (lineFlash) {
    flashLines();
    if (millis()-flashTimer >= 300+(lineTimers.size()-1)*40) {
      flashTimer=millis();
      println("hey");
      lineFlash=false;
      clearedRows=null;
      gravity();
    }
  }

  pushMatrix();
  pushStyle();
  translate(horizSpacer, vertSpacer+boardHeight+20);
  textSize(14);
  fill(255);
  String string = "controls:\n"+
    "move left: left arrow / NUMPAD_4\n"+
    "move right: right arrow / NUMPAD_6\n"+
    "soft drop: down arrow / NUMPAD_2\n"+
    "hard drop: spacebar / NUMPAD_5\n"+
    "rotate left: Z / NUMPAD_1/7\n"+
    "rotate right: X / NUMPAD_3/9\n"+
    "pause: P\n"+
    "quit: ESC";
  text(string, 0, 0);
  popMatrix();
  if (!started) {
    pushMatrix();
    translate(width/2,height/2);
    textSize(32);
    textAlign(CENTER);
    PFont font = loadFont("Assistant-ExtraBold-72.vlw");
    textFont(font);
    text("To start press ENTER", 0, 0);
    popMatrix();
  }
  popStyle();
}
int flashTimer=0;
HashMap<Integer, Integer> lineTimers = new HashMap();

public void flashLines() {
  if (clearedRows==null) return;

  //lineFlash = true;
  //lineTimers = new int[clearedRows.length];
  for (Integer i : clearedRows) {
    println(millis());
    println(millis());
    lineTimers.put(i, millis() - flashTimer + i*40);
  }
  for (Integer i : clearedRows) {
    for (Tile tile : board[i]) {
      board[tile.y][tile.x] = new Tile(lerpColor(color(255), gray, map(lineTimers.get(i), i*40, 300+i*40, 0, 1)), tile.y, tile.x, false);
    }
  }
}

Integer[] clearedRows;

public void clearLines() {
  //ArrayList<Tile[]> rows = new ArrayList();
  HashMap<Integer, Tile[]> rows = new HashMap();
  for (int i = board.length-1; i >= 0; i--) {
    boolean filled = true;
    for (Tile tile : board[i]) {
      if (tile.Empty()) filled = false;
    }
    if (filled) {
      rows.put(i, board[i]);
    }
  }
  //int m=-1;
  if (!rows.isEmpty()) {

    clearedRows = rows.keySet().toArray(new Integer[rows.keySet().size()]); 
    for (Integer row : clearedRows) {
      for (Tile tile : board[row]) {
        board[row][tile.x] = new Tile(gray, row, tile.x, false);
      }
    }
    println("start");
    flashTimer=millis();
    //flashLines();
    lineFlash = true;
    //gravity();
  }
  segments.clear();
}


public void gravity() {
  for (int i = board.length-1; i >= 0; i--) {

    for (int j = 0; j< board[0].length; j++) {
      //if currect tile is not a part of any shape in "segments" - do: {
      boolean contains = false;
      for (ArrayList<Tile> segment : segments) {
        if (segment.contains(board[i][j])) contains = true;
      }
      if (!contains) {
        flood(i, j);
        if (!nodes.isEmpty())
          segments.add((ArrayList<Tile>)nodes.clone());
        nodes.clear();
      }
    }
  }
  boolean canFall = true;
  while (canFall) {
    boolean shapeFell = false;
    for (ArrayList<Tile> segment : segments) {
      Shape _shape = new Shape(segment);
      while (_shape.Fall()) {
        shapeFell = true;
      }
      if (!shapeFell) {
        canFall = false;
      }
    }
  }
  for (ArrayList<Tile> segment : segments) {
    for (Tile tile : segment) {
      board[tile.y][tile.x] = new Tile(tile.colour, tile.y, tile.x, true);
    }
  }
}

private ArrayList<Tile> nodes = new ArrayList(); 

public void flood(int i, int j) {
  if (j<0)return; 
  if (i<0)return; 
  if (j>=board[0].length)return; 
  if (i>=board.length)return; 

  if (board[i][j].Empty()) return; 
  if (nodes.contains(board[i][j]))return; 

  nodes.add(board[i][j]); 

  flood(i, j+1); 
  flood(i, j-1); 
  flood(i+1, j); 
  flood(i-1, j);
}

public void showGhost() {
  ArrayList<Tile> copy = new ArrayList(); 
  for (Tile tile : shape.points) {
    copy.add(tile.clone(false));
  }

  while (shape.Fall()) {
  }

  if (shape.sameLoc(copy)) { 
    return;
  }

  ghost = new Shape(shape.points); 
  for (Tile tile : ghost.points) {
    boolean originalContainsTile = false; 
    for (Tile copyTile : copy) {
      if (copyTile.x == tile.x && copyTile.y == tile.y) {
        originalContainsTile=true;
      }
    }
    if (!originalContainsTile) {
      tile.colour = ghostGlow; 
      board[tile.y][tile.x] = new Tile(ghostGlow, tile.y, tile.x, false);
    }
  }
  for (Tile tile : shape.points) {
    tile.setFull(false);
  }
  shape.points = copy; 

  try {
    shape.setPivot(shape.getPivotIndex(shape.type));
  }
  catch(ArrayIndexOutOfBoundsException e) {
  }
  try {
    for (int i = 0; i<4; i++) {
      Tile ghostTile = ghost.points.get(i), shapeTile = shape.points.get(i); 

      board[shapeTile.y][shapeTile.x] = shapeTile; 
      board[ghostTile.y][ghostTile.x] = ghostTile;
    }
  }
  catch(NullPointerException e) {
  }
}


public void spawnShape() {
  shape = new Shape(shapes[floor(random(shapes.length))], board);
}

public void Lose() {
  println("Loser!");
}

public void mousePressed() {
  if (mouseX>=horizSpacer && mouseX< horizSpacer+boardWidth
    && mouseY>=vertSpacer&&mouseY<vertSpacer+boardHeight) {
    int _mouseX = PApplet.parseInt(map(mouseX, horizSpacer, horizSpacer+boardWidth, 0, board[0].length)); 
    int _mouseY = PApplet.parseInt(map(mouseY, vertSpacer, vertSpacer+boardHeight, 0, board.length)); 

    if (board[_mouseY][_mouseX].Full()) {
      flood(_mouseY, _mouseX); 
      for (int i = 0; i< nodes.size(); i++) {
        int col = (int)map(i, 0, nodes.size()-1, 0, 255); 
        nodes.get(i).colour=color(col, col, 255);
      }
      nodes.clear();
    }
  }
}


public void keyPressed() {
  //if (key=='i'||key=='j'||key=='l'||key=='o'||key=='s'||key=='t'||key=='z') {
  //  shape = new  Shape(key, board);
  //}

  if (key=='p') {
    if (shape!=null) {
      running = !running;
      pausedText=!pausedText;
    }
  }

  if (key == ENTER) {
    if (!running && shape==null) {
      started = true;
      spawnShape(); 
      running = true;
    }
  }



  if (key=='h')  helperText=!helperText; 


  //println(keyCode); 
  if (running) {

    if (keyCode == 32 || keyCode == 101) {
      if (shape!=null) {
        shape.drop(); 

        timer = millis() - speed / 2;
      }
    }

    if (shape!=null)
      if (shape.pivot != null) {
        if (key == 'z'|| keyCode == 97|| keyCode == 103) {
          shape.rotate(0);
        } else if (key == 'x'|| keyCode == 99|| keyCode == 105) {
          shape.rotate(1);
        }
      }



    if (shape != null) {
      if (keyCode==DOWN || keyCode == 98) {

        downHeld = true;
      }
      if (keyCode==LEFT || keyCode == 100) {
        shape.move(-1);
      }
      if (keyCode==RIGHT|| keyCode == 102) {
        shape.move(1);
      }
    }
  }
}

public void keyReleased() {


  if (keyCode == DOWN || keyCode == 98) {
    downHeld = false;
  }
}
public class Shape { //<>//
  int yellow = color(255, 253, 66), cyan=color(38, 255, 254), 
    blue=color(0, 43, 249), orange = color(254, 162, 46), 
    green=color(42, 253, 62), red=color(253, 0, 25), 
    purple = 0xffba51ff;

  char type;
  int _color;
  ArrayList<Tile> points = new ArrayList();
  Tile pivot;

  public boolean Fall() {
    boolean val=true;

    ArrayList<Tile> copy = (ArrayList<Tile>) points.clone();


    for (Tile p : copy) {
      if (fallBlocked(p)) {
        val= false;
      }
    }
    if (val) {
      for (Tile p : copy) {
        board[p.y][p.x] = new Tile(gray, p.y, p.x, false);
      }
      for (Tile p : copy) {
        p.y++;
      }
    }
    for (Tile p : copy) {
      board[p.y][p.x] = new Tile(_color, p.y, p.x, true);
    }

    points = copy;
    return val;
  }

  public boolean fallBlocked(Tile tile) {
    boolean val = true;
    if (tile.y+1 >= board.length) {
      return true;
    } else {
      if (board[tile.y+1][tile.x].Full()) {
        if (board[tile.y+1][tile.x]!=null) {
          for (Tile block : points) {
            if (block.x == tile.x && block.y == tile.y+1) {
              val = false;
            }
          }
        }
      } else val = false;
    }

    return val;
  }

  public boolean rotate(int dir) {
    ArrayList<Tile> recoverPoints = new ArrayList();
    for (Tile tile : points) {
      recoverPoints.add(tile.clone(true));
    }
    PVector[] R;

    if (dir==0) { 
      R = new PVector[] {new PVector(0, -1), new PVector(1, 0)};
    } else if (dir==1) {
      R = new PVector[] {new PVector(0, 1), new PVector(-1, 0)};
    } else R = null;


    PVector P = new PVector(pivot.x, pivot.y);
    for (Tile tile : points) {
      PVector V = new PVector(tile.x, tile.y);
      PVector Vr = V.sub(P);
      PVector Vt = new PVector();
      Vt.y = R[0].x*Vr.y+R[0].y*Vr.x;
      Vt.x = R[1].x*Vr.y+R[1].y*Vr.x;
      V= Vt.add(P);
      if (PApplet.parseInt(V.x)<board[0].length && PApplet.parseInt(V.y)<board.length && PApplet.parseInt(V.x)>=0 &&
        PApplet.parseInt(V.y)>=0 && board[PApplet.parseInt(V.y)][PApplet.parseInt(V.x)].Empty()) {

        tile.x = PApplet.parseInt(V.x);
        tile.y = PApplet.parseInt(V.y);
      } else {
        for (int i = 0; i<4; i++) {
          points.get(i).x = recoverPoints.get(i).x;
          points.get(i).y = recoverPoints.get(i).y;
        }
        return false;
      }
    }

    for (int i = 0; i<4; i++) {
      board[recoverPoints.get(i).y][recoverPoints.get(i).x] = 
        new Tile(gray, recoverPoints.get(i).y, recoverPoints.get(i).x, false);
    }
    for (int i = 0; i<4; i++) {
      board[points.get(i).y][points.get(i).x] = 
        new Tile(_color, points.get(i).y, points.get(i).x, true);
    }
    return true;
  }

  public void move(int dir) {
    boolean val=true;

    ArrayList<Tile> copy = (ArrayList<Tile>) points.clone();


    for (Tile p : copy) {
      if (moveBlocked(dir)) {
        val= false;
      }
    }
    if (val) {
      for (Tile p : copy) {
        board[p.y][p.x] = new Tile(120, p.y, p.x, false);
      }
      for (Tile p : copy) {
        p.x+=dir;
      }
    }
    for (Tile p : copy) {
      board[p.y][p.x] = new Tile(_color, p.y, p.x, true);
    }

    points = copy;
  }

  public boolean moveBlocked(int dir) {
    boolean val = true;
    for (Tile tile : points) {
      if (tile.x+dir >= board[0].length || tile.x+dir < 0) {
        return true;
      } else {
        if (board[tile.y][tile.x+dir].Full()) {
          if (board[tile.y][tile.x+dir]!=null) {
            boolean friendlyFire = false;
            for (Tile block : points) {
              if (block.x == tile.x+dir && block.y == tile.y) {
                friendlyFire = true;
              }
            }
            if (!friendlyFire) {
              return true;
            }
          }
        } else val = false;
      }
    }
    return val;
  }

  Shape(char type, Tile[][] board) {
    this.type = type;
    boolean clear = false;
    ArrayList<Integer> indexes = new ArrayList();
    switch(type) {

    case 'i':
      {
        for (int j = 0; j<=board[0].length-4; j++) {
          if (board[0][j].Empty() && board[0][j+1].Empty() 
            && board[0][j+2].Empty() && board[0][j+3].Empty()) {
            clear = true;
            indexes.add(j);
          }
        } 
        if (!clear) {
          Lose();
          break;
        }
        _color = cyan; 
        final int index = indexes.get(floor(random(indexes.size())));
        if (clear) {
          for (int j = index; j<index+4; j++) {
            points.add(new Tile(cyan, 0, j, true)); 
            board[0][j] = points.get(points.size()-1);
          }
          pivot = points.get(1);
        }
      }
      break;
      //////////////////
    case 'o':
      {
        for (int j = 0; j<=board[0].length-2; j++) {
          if (board[0][j].Empty() && board[0][j+1].Empty() 
            && board[1][j].Empty() && board[1][j+1].Empty()) {
            clear = true;
            indexes.add(j);
          }
        }
        if (!clear) {
          Lose();
          break;
        }
        _color =yellow;
        final int index = indexes.get(floor(random(indexes.size())));
        if (clear) {
          for (int i = 0; i < 2; i++) {
            for (int j = index; j < index+2; j++) {
              points.add(new Tile(yellow, i, j, true));
              board[i][j] = points.get(points.size()-1);
            }
          }
        }
      }
      break;
      //////////////////
    case 'j':
      {
        for (int j = 0; j<=board[0].length-3; j++) {
          if (board[0][j].Empty() && board[0][j+1].Empty() 
            && board[0][j+2].Empty() && board[1][j+2].Empty()) {
            clear = true;
            indexes.add(j);
          }
        } 
        if (!clear) {
          Lose();
          break;
        }
        _color =blue;
        final int index = indexes.get(floor(random(indexes.size())));
        if (clear) {
          for (int j = index; j<index+3; j++) {
            points.add(new Tile(blue, 0, j, true));
            board[0][j] = points.get(points.size()-1);
            if (j==index+2) {
              points.add(new Tile(blue, 1, j, true));
              board[1][j] = points.get(points.size()-1);
            }
          }
          pivot = points.get(1);
        }
      }
      break;
      //////////////////
    case 'l':
      {
        for (int j = 0; j<=board[0].length-3; j++) {
          if (board[0][j].Empty() && board[0][j+1].Empty() 
            && board[0][j+2].Empty() && board[1][j].Empty()) {
            clear = true;
            indexes.add(j);
          }
        } 
        if (!clear) {
          Lose();
          break;
        }
        _color =orange;
        final int index = indexes.get(floor(random(indexes.size())));
        if (clear) {
          for (int j = index; j<index+3; j++) {
            points.add( new Tile(orange, 0, j, true));
            board[0][j] = points.get(points.size()-1);
            if (j==index) {
              points.add(new Tile(orange, 1, j, true));
              board[1][j] = points.get(points.size()-1);
            }
          }
          pivot = points.get(2);
        }
      }
      break;
      //////////////////
    case 's':
      {
        for (int j = 0; j<=board[0].length-3; j++) {
          if (board[0][j+1].Empty() && board[0][j+2].Empty() 
            && board[1][j].Empty() && board[1][j+1].Empty()) {
            clear = true;
            indexes.add(j);
          }
        } 
        if (!clear) {
          Lose();
          break;
        }
        _color =green;
        final int index = indexes.get(floor(random(indexes.size())));
        if (clear) {
          for (int i = 0; i <2; i++) {
            for (int j = index; j<=index+1; j++) {
              if (i==0) {
                points.add(new Tile(green, i, j+1, true));
                board[i][j+1] = points.get(points.size()-1);
              } else {
                points.add(new Tile(green, i, j, true));
                board[i][j]= points.get(points.size()-1);
              }
            }
          }
          pivot = points.get(3);
        }
      }
      break;
      //////////////////
    case 'z':
      {
        for (int j = 0; j<=board[0].length-3; j++) {
          if (board[0][j].Empty() && board[0][j+1].Empty() 
            && board[1][j+1].Empty() && board[1][j+2].Empty()) {
            clear = true;
            indexes.add(j);
          }
        } 
        if (!clear) {
          Lose();
          break;
        }
        _color =red;
        final int index = indexes.get(floor(random(indexes.size())));
        if (clear) {
          for (int i = 0; i <2; i++) {
            for (int j = index; j<=index+1; j++) {
              if (i==1) {
                points.add(new Tile(red, i, j+1, true));
                board[i][j+1] = points.get(points.size()-1);
              } else {
                points.add(new Tile(red, i, j, true));
                board[i][j] = points.get(points.size()-1);
              }
            }
          }
          pivot = points.get(1);
        }
      }
      break;
      //////////////////
    case 't':
      {
        for (int j = 0; j<=board[0].length-3; j++) {
          if (board[0][j].Empty() && board[0][j+1].Empty() 
            && board[0][j+2].Empty() && board[1][j+1].Empty()) {
            clear = true;
            indexes.add(j);
          }
        } 
        if (!clear) {
          Lose();
          break;
        }
        _color =purple;
        final int index = indexes.get(floor(random(indexes.size())));
        if (clear) {
          for (int j = index; j<index+3; j++) {      
            points.add(new Tile(purple, 0, j, true));
            board[0][j] = points.get(points.size()-1);
          }
          points.add(new Tile(purple, 1, index+1, true));
          board[1][index+1] = points.get(points.size()-1);

          pivot = points.get(1);
        }
      }
      break;
      //////////////////
    }
  }

  //Constructor for segment flooding when line clears - gravity ensues
  Shape(ArrayList<Tile> tiles) {
    for (Tile t : tiles) {
      this.points.add(t);
      t.setFull(true);
    }
  }
  //char type;
  //color _color;
  //ArrayList<Tile> points = new ArrayList();
  //Tile pivot;

  public void drop() {
    while (Fall()) {
    }
  }

  public void setPivot(int index) {
    this.pivot = this.points.get(index);
  }

  public int getPivotIndex(char var) {
    switch(var) {
    case 'i': 
      return 1;
    case 'o': 
      return -1;
    case 'j': 
      return 1;
    case 'l': 
      return 2;
    case 's': 
      return 3;
    case 'z': 
      return 1;
    case 't': 
      return 1;
    default: 
      return -1;
    }
  }

  public boolean sameLoc(Shape shp) {
    if (shp.points.size()>4)throw new IllegalArgumentException();
    boolean same = true;
    for (Tile tile : this.points) {
      boolean contains = false;
      for (Tile otherTile : shp.points) {
        if (tile.x == otherTile.x && tile.y == otherTile.y) contains = true;
      }
      if (!contains) {
        same = false;
      }
    }
    return same;
  }

  public boolean sameLoc(ArrayList<Tile> otherPoints) {
    if (otherPoints.size()>4)throw new IllegalArgumentException();
    boolean same = true;
    for (Tile tile : this.points) {
      boolean contains = false;
      for (Tile otherTile : otherPoints) {
        if (tile.x == otherTile.x && tile.y == otherTile.y) contains = true;
      }
      if (!contains) {
        same = false;
      }
    }
    return same;
  }
}
public class Tile {
  public int colour;
  private boolean full;
  public int x, y;
  Tile(int col, int i, int j, boolean block) {
    colour = col;
    full=block;
    x=j;
    y=i;
  }
  public int getColor() {
    return this.colour;
  }

  public boolean Empty() {
    return !full;
  }
  public boolean Full() {
    return full;
  }

  public void setFull(boolean val) {
    full = val;
  }

  public Tile clone(boolean val) {
    return new Tile(colour, y, x, val);
  }
  
  
}
  public void settings() {  fullScreen(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--present", "--window-color=#666666", "--stop-color=#cccccc", "tetris" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
