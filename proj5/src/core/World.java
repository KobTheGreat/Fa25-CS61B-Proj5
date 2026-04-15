package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.princeton.cs.algs4.WeightedQuickUnionUF;

public class World {
    private static final int WIDTH = 60;
    private static final int HEIGHT = 30;
    private static final int AREA = WIDTH * HEIGHT;
    //private static final long SEED = 8678425772563348927L;
    //private static final Random RANDOM = new Random(SEED);
    private static final int MAX_WIDTH = (int) (Math.sqrt(WIDTH) * 2); //包括两边墙，下同
    private static final int MAX_HEIGHT = (int) (Math.sqrt(HEIGHT) * 2);
    private static final int MIN_WIDTH = 4;
    private static final int MIN_HEIGHT = 4;
    private static final long[] vis = new long[HEIGHT];
    private int taken_area;
    private int room_cnt;
    private final List<room> rooms = new ArrayList<>();
    private Random RANDOM;

    static class room {
        int x, y;
        public room(int a, int b) {
            x = a;
            y = b;
        }
    }

    /**
     * Constructor for World with a given seed
     */
    public World(long seed) {
        RANDOM = new Random(seed);
        // 重置所有静态变量
        taken_area = 0;
        room_cnt = 0;
        rooms.clear();
        for (int i = 0; i < HEIGHT; i++) {
            vis[i] = 0;  // 重置vis数组
        }
    }

    /**
     * Get the width of the world
     */
    public static int getWidth() {
        return WIDTH;
    }

    /**
     * Get the height of the world
     */
    public static int getHeight() {
        return HEIGHT;
    }

    public static boolean check_overlap(int x, int w, int y, int h) {
        for (int i = -1; i < h + 1; i++) {
            if (y + i < 0 || y + i >= HEIGHT) {
                continue;
            }
            long temp = (((1L << (x + w)) - 1) ^ ((1L << x) - 1));
            if (((vis[y + i] & temp) | ((vis[y + i] << 1) & temp) | ((vis[y + i] >> 1) & temp)) != 0) {
                return true;
            }
        }
        return false;
    }

    public static void create_room(int x, int w, int y, int h, TETile[][] tiles) {
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (i == 0 || i == w - 1 || j == 0 || j == h - 1) {
                    tiles[x + i][y + j] = Tileset.WALL;
                    continue;
                }
                tiles[x + i][y + j] = Tileset.FLOOR;
            }
        }
    }

    public void create_rooms(TETile[][] tiles) {
        while (taken_area < AREA * 0.45) { //0.5的话会导致部分种子加载不出地图（为了美观overlap写得较苛刻，腾不出地方放room了）
            int x = RANDOM.nextInt(WIDTH - MIN_WIDTH);
            int y = RANDOM.nextInt(HEIGHT - MIN_HEIGHT);
            //(x, y)代表当前房间的左下角坐标（墙角）
            int w = RANDOM.nextInt(MIN_WIDTH, Math.min(MAX_WIDTH, WIDTH - x) + 1);
            int h = RANDOM.nextInt(MIN_HEIGHT, Math.min(MAX_HEIGHT, HEIGHT - y) + 1);
            if (check_overlap(x, w, y, h)) {
                continue;
            }
            create_room(x, w, y, h, tiles);
            taken_area += w * h;
            for (int i = 0; i < h; i++) {
                vis[y + i] |= (((1L << (x + w)) - 1) ^ ((1L << x) - 1)); //第y+i行从x到x+w-1全部标记成被占用
            }
            rooms.add(new room(RANDOM.nextInt(x + 1, x + w - 1), RANDOM.nextInt(y + 1, y + h - 1)));
            room_cnt++;
        }
    }

    public void connect_rooms(TETile[][] tiles) {
        WeightedQuickUnionUF uf = new WeightedQuickUnionUF(room_cnt);
        for (int cnt = 0; cnt < room_cnt - 1; cnt++) { //烦死了直接上暴力
            int min_dis = WIDTH + HEIGHT;
            int u = -1, v = -1;
            for (int i = 0; i < room_cnt; i++) {
                for (int j = i + 1; j < room_cnt; j++) {
                    if (!uf.connected(i, j)) {
                        room aa = rooms.get(i);
                        room bb = rooms.get(j);
                        int dis = Math.abs(aa.x - bb.x) + Math.abs(aa.y - bb.y);
                        if (dis < min_dis) {
                            min_dis = dis;
                            u = i;
                            v = j;
                        }
                    }
                }
            }
            uf.union(u, v); //就决定是你了！！
            connect_2(rooms.get(u), rooms.get(v), tiles);
        }
    }

    public static void connect_2(room p, room q, TETile[][] tiles) {
        int x1 = p.x;
        int y1 = p.y;
        int x2 = q.x;
        int y2 = q.y;
        int currX = x1;
        int currY = y1;
        while (currX != x2) {
            dig(currX, currY, tiles); //横
            currX += (x2 > currX) ? 1 : -1;
        }
        while (currY != y2) {
            dig(currX, currY, tiles); //竖
            currY += (y2 > currY) ? 1 : -1;
        }
    }

    public static void dig(int x, int y, TETile[][] tiles) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if ((i != 0 || j != 0) && tiles[x + i][y + j] == Tileset.NOTHING) {
                    tiles[x + i][y + j] = Tileset.WALL;
                }
            }
        }
        tiles[x][y] = Tileset.FLOOR;
    }
    /**
     * Generate the world and return the tiles array
     * @return the generated world tiles
     */
    public TETile[][] generate() {
        TETile[][] tiles = new TETile[WIDTH][HEIGHT];
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                tiles[i][j] = Tileset.NOTHING;
            }
        }
        create_rooms(tiles);
        connect_rooms(tiles);
        return tiles;
    }

    /**
     * Find the first floor tile (bottommost-leftmost) for avatar starting position
     * @param tiles the world tiles
     * @return array [x, y] of avatar starting position, or null if no floor found
     */
    public static int[] findAvatarStart(TETile[][] tiles) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (tiles[x][y].equals(Tileset.FLOOR)) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    public static int[] getRandomPos(TETile[][] tiles) {
        while (true) {
            int x = (int) (Math.random() * WIDTH);
            int y = (int) (Math.random() * HEIGHT);
            if (tiles[x][y].equals(Tileset.FLOOR)) {
                return new int[]{x, y};
            }
        }
    }

    /**
     * Get the Random object for saving/loading state
     */
    public Random getRandom() {
        return RANDOM;
    }

}
