package com.middlerim.server.command.storage.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.middlerim.server.MessageCommands;
import com.middlerim.server.command.Config;
import com.middlerim.session.SessionId;

public class VpTree<T extends TreePoint<T>> implements LocationStorage<T> {

  private static final int MAX_LEAF_SIZE = 30;
  private static final int VANTAGE_POINT_CANDIDATES = 10;
  private static final int TEST_POINT_COUNT = 10;

  private VpTreeNode<T> root;
  Map<SessionId, T> userLocationMap = new HashMap<>(Config.MAX_ACTIVE_SESSION_SIZE);

  public int size() {
    return size(root);
  }

  private int size(VpTreeNode<T> node) {
    int size = 0;
    if (node.left != null) {
      size += size(node.left);
    }
    if (node.leaves != null) {
      size += node.leaves.size();
    }
    if (node.right != null) {
      size += size(node.right);
    }
    return size;
  }

  @Override
  public T findBySessionId(SessionId sessionId) {
    return userLocationMap.get(sessionId);
  }

  @Override
  public boolean remove(SessionId sessionId) {
    T user = findBySessionId(sessionId);
    if (user == null) {
      return false;
    }
    return remove(user);
  }

  @Override
  public void clearAll() {
    root = null;
    userLocationMap.clear();
  }

  @Override
  public List<T> findAround(SessionId sessionId, byte radius) {
    T user = findBySessionId(sessionId);
    if (user == null) {
      return Collections.emptyList();
    }
    int meter = MessageCommands.toMeter(radius);
    return findAround(user, meter);
  }

  public List<T> findAround(T point, int maxDistanceMeter) {
    List<T> result = new ArrayList<>();
    findAround(root, point, maxDistanceMeter, result);
    return result;
  }
  private void findAround(VpTreeNode<T> baseNode, T point, int maxDistanceMeter, List<T> result) {
    if (baseNode == null) {
      return;
    }
    if (baseNode.leaves != null) {
      for (T p : baseNode.leaves) {
        if (point.session().sessionId.equals(p.session().sessionId)) {
          continue;
        }
        if (point.point().distanceMeter(p.point()) <= maxDistanceMeter) {
          result.add(p);
        }
      }
      return;
    }
    boolean needLeft = true;
    int distanceToLeftCenter = baseNode.vantagePoint.point().distanceMeter(point.point());
    if (distanceToLeftCenter + baseNode.leftRadiusMeter <= maxDistanceMeter) {
      needLeft = false;
      baseNode.left.getAll(result);
    }
    if (needLeft && distanceToLeftCenter + maxDistanceMeter < baseNode.leftRadiusMeter) {
      findAround(baseNode.left, point, maxDistanceMeter, result);
    } else if (distanceToLeftCenter - maxDistanceMeter >= baseNode.leftRadiusMeter) {
      findAround(baseNode.right, point, maxDistanceMeter, result);
    } else {
      if (needLeft) {
        findAround(baseNode.left, point, maxDistanceMeter, result);
      }
      findAround(baseNode.right, point, maxDistanceMeter, result);
    }
  }

  @Override
  public void put(T point) {
    remove(point);
    if (root == null) {
      ArrayList<T> list = new ArrayList<>(MAX_LEAF_SIZE);
      list.add(point);
      root = buildTreeNode(list, 0);
    } else {
      VpTreeNode<T> replacement = add(root, point);
      if (replacement != null) {
        root = replacement;
      }
    }
    userLocationMap.put(point.session().sessionId, point);
  }

  private VpTreeNode<T> add(VpTreeNode<T> baseNode, T point) {
    if (baseNode.leaves == null) {
      int distanceToLeftCenter = baseNode.vantagePoint.point().distanceMeter(point.point());
      boolean left = distanceToLeftCenter <= baseNode.leftRadiusMeter;
      VpTreeNode<T> replacement = add(left ? baseNode.left : baseNode.right, point);
      if (replacement != null) {
        if (left) {
          baseNode.left = replacement;
        } else {
          baseNode.right = replacement;
        }
      }
      return null;
    } else {
      baseNode.leaves.add(point);
      if (baseNode.leaves.size() <= MAX_LEAF_SIZE) {
        return null;
      }
      return buildTreeNode(baseNode.leaves, 0);
    }
  }

  private static final int REMOVE_MODIFIED = 1;
  private static final int REMOVE_KILL_PARENT = 2;
  public boolean remove(T point) {
    T cache = userLocationMap.remove(point.session().sessionId);
    if (cache == null) {
      return false;
    }
    int result = remove(root, point);
    if (result == REMOVE_KILL_PARENT) {
      root = null;
    }
    return result > 0;
  }

  private int remove(VpTreeNode<T> baseNode, T point) {
    if (baseNode.leaves == null) {
      int distanceToLeftCenter = baseNode.vantagePoint.point().distanceMeter(point.point());
      VpTreeNode<T> childNode = distanceToLeftCenter <= baseNode.leftRadiusMeter ? baseNode.left : baseNode.right;
      int result = remove(childNode, point);
      if (result == REMOVE_KILL_PARENT) {
        if (childNode == baseNode.right) {
          baseNode.right = null;
        } else {
          if (baseNode.right == null) {
            baseNode.left = null;
            baseNode.vantagePoint = null;
            return result;
          }
        }
      }
      return result > 0 ? REMOVE_MODIFIED : 0;
    } else {
      boolean modified = baseNode.leaves.remove(point);
      if (baseNode.leaves.isEmpty()) {
        return REMOVE_KILL_PARENT;
      }
      return modified ? REMOVE_MODIFIED : 0;
    }
  }

  public static <T extends TreePoint<T>> VpTree<T> buildVpTree(List<T> points) {
    VpTree<T> tree = new VpTree<>();
    tree.root = buildTreeNode(points, 0);
    for (T p : points) {
      tree.userLocationMap.put(p.session().sessionId, p);
    }
    return tree;
  }

  private static class DistanceHolder<T extends TreePoint<T>> implements Comparable<DistanceHolder<T>> {
    final int distance;
    final T p;

    private DistanceHolder(int distance, T p) {
      this.distance = distance;
      this.p = p;
    }
    @Override
    public int compareTo(DistanceHolder<T> o) {
      return distance < o.distance ? -1 : 1;
    }
    @Override
    public String toString() {
      return "" + distance + p;
    }
  }

  private static <T extends TreePoint<T>> VpTreeNode<T> buildTreeNode(List<T> points, int level) {
    level++;
    VpTreeNode<T> node = new VpTreeNode<T>();
    if (points.size() <= MAX_LEAF_SIZE) {
      node.leaves = points;
      return node;
    }
    T basePoint = chooseNewVantagePoint(points);
    TreeSet<DistanceHolder<T>> sorted = new TreeSet<>();

    for (int i = 0; i < points.size(); ++i) {
      T t = points.get(i);
      sorted.add(new DistanceHolder<>(basePoint.point().distanceMeter(t.point()), t));
    }
    int middle = points.size() / 2;
    List<T> leftPoints = new ArrayList<>(middle + 1);
    List<T> rightPoints = new ArrayList<>(middle);
    int medianDistance = -1;
    for (DistanceHolder<T> h : sorted) {
      if (middle == 0) {
        medianDistance = h.distance;
      }
      if (middle-- >= 0) {
        leftPoints.add(h.p);
      } else if (h.distance == medianDistance) {
        leftPoints.add(h.p);
      } else {
        rightPoints.add(h.p);
      }
    }

    node.vantagePoint = basePoint;
    node.leftRadiusMeter = medianDistance;

    try {
      if (!leftPoints.isEmpty()) {
        if (medianDistance <= MessageCommands.MIN_METER) {
          node.left = new VpTreeNode<T>();
          node.left.leaves = leftPoints;
        } else if (leftPoints.size() > MAX_LEAF_SIZE && rightPoints.isEmpty()) {
          node.left = new VpTreeNode<T>();
          node.left.leaves = leftPoints;
        } else {
          node.left = buildTreeNode(leftPoints, level);
        }
      }
      if (!rightPoints.isEmpty()) {
        node.right = buildTreeNode(rightPoints, level);
      }
      if (node.left == null && node.right != null) {
        throw new IllegalStateException();
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected exception", e);
    }

    return node;
  }

  private static <T extends TreePoint<T>> T chooseNewVantagePoint(List<T> points) {
    List<T> candidates = new ArrayList<>(VANTAGE_POINT_CANDIDATES);
    List<T> testPoints = new ArrayList<>(TEST_POINT_COUNT);

    for (int i = 0; i < VANTAGE_POINT_CANDIDATES; ++i) {
      int basePointIndex = i + (int) (Math.random() * (points.size() - i));
      T candidate = points.get(basePointIndex);
      candidates.add(candidate);
    }

    for (int i = VANTAGE_POINT_CANDIDATES; i < VANTAGE_POINT_CANDIDATES + TEST_POINT_COUNT; ++i) {
      int testPointIndex = i + (int) (Math.random() * (points.size() - i));
      T testPoint = points.get(testPointIndex);
      testPoints.add(testPoint);
    }

    T bestBasePoint = candidates.get(0);
    long bestBasePointSigma = 0;

    for (T basePoint : candidates) {
      int distances[] = new int[TEST_POINT_COUNT];
      for (int i = 0; i < TEST_POINT_COUNT; ++i) {
        distances[i] = basePoint.point().distanceMeter(testPoints.get(i).point());
      }
      long sigma = sigmaSquare(distances);
      if (sigma > bestBasePointSigma) {
        bestBasePointSigma = sigma;
        bestBasePoint = basePoint;
      }
    }

    return bestBasePoint;
  }

  private static long sigmaSquare(int[] values) {
    long sum = 0;

    for (long value : values) {
      sum += value;
    }

    long avg = sum / values.length;
    long sigmaSq = 0;

    for (int value : values) {
      long dev = value - avg;
      sigmaSq += dev * dev;
    }

    return sigmaSq;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString(root, 0, sb);
    return sb.toString();
  }

  private String levelPadding(int level) {
    char[] cs = new char[level * 2];
    Arrays.fill(cs, '-');
    return new String(cs);
  }
  private void toString(VpTreeNode<T> node, int level, StringBuilder sb) {
    sb.append(node.vantagePoint == null ? "(N/A)" : node.vantagePoint).append("#").append(node.leftRadiusMeter).append(System.lineSeparator());
    if (node.left != null) {
      sb.append(levelPadding(level)).append(" ").append(level).append("-L ");
      toString(node.left, level + 1, sb);
    }
    if (node.leaves != null) {
      sb.append(levelPadding(level)).append("- Leaves:").append(node.leaves.size()).append(System.lineSeparator());
      for (T n : node.leaves) {
        sb.append(levelPadding(level + 1)).append(n).append(System.lineSeparator());
      }
    }
    if (node.right != null) {
      sb.append(levelPadding(level)).append(" ").append(level).append("-R ");
      toString(node.right, level + 1, sb);
    }
  }
}
