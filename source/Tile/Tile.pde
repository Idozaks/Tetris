public class Tile {
  public color colour;
  private boolean full;
  public int x, y;
  Tile(color col, int i, int j, boolean block) {
    colour = col;
    full=block;
    x=j;
    y=i;
  }
  public color getColor() {
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
