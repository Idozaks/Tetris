public class Shape { //<>//
  color yellow = color(255, 253, 66), cyan=color(38, 255, 254), 
    blue=color(0, 43, 249), orange = color(254, 162, 46), 
    green=color(42, 253, 62), red=color(253, 0, 25), 
    purple = #ba51ff;

  char type;
  color _color;
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

  boolean fallBlocked(Tile tile) {
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

  boolean rotate(int dir) {
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
      if (int(V.x)<board[0].length && int(V.y)<board.length && int(V.x)>=0 &&
        int(V.y)>=0 && board[int(V.y)][int(V.x)].Empty()) {

        tile.x = int(V.x);
        tile.y = int(V.y);
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

  void move(int dir) {
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

  boolean moveBlocked(int dir) {
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

  void drop() {
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
