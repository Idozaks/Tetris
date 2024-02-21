public Tile[][] board = new Tile[21][12]; //<>//
int boardCols = board[0].length, boardRows = board.length;
int tileWidth = 30;
Shape shape, ghost;
public color gray = #747474, ghostGlow=#adadad;
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

void setup() {
  //size(800, 1000);
  fullScreen();
  for (int i = 0; i < 21; i++) {
    for (int j = 0; j < 12; j++) {
      board[i][j] = new Tile(gray, i, j, false);
    }
  }
}

void draw() {
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

void flashLines() {
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

void clearLines() {
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


void gravity() {
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

void showGhost() {
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

void mousePressed() {
  if (mouseX>=horizSpacer && mouseX< horizSpacer+boardWidth
    && mouseY>=vertSpacer&&mouseY<vertSpacer+boardHeight) {
    int _mouseX = int(map(mouseX, horizSpacer, horizSpacer+boardWidth, 0, board[0].length)); 
    int _mouseY = int(map(mouseY, vertSpacer, vertSpacer+boardHeight, 0, board.length)); 

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


void keyPressed() {
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

void keyReleased() {


  if (keyCode == DOWN || keyCode == 98) {
    downHeld = false;
  }
}
