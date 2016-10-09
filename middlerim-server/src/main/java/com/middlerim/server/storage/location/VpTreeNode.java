package com.middlerim.server.storage.location;

import java.util.List;

public final class VpTreeNode<T extends TreePoint<T>> {

  List<T> leaves;

  VpTreeNode<T> left;
  VpTreeNode<T> right;
  T vantagePoint;
  int leftRadiusMeter;

  public void getAll(List<T> acc) {
    if (leaves != null) {
      acc.addAll(leaves);
      return;
    }
    if (left != null) {
      left.getAll(acc);
    }
    if (right != null) {
      right.getAll(acc);
    }
  }
}
