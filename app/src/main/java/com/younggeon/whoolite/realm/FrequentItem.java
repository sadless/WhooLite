package com.younggeon.whoolite.realm;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Created by sadless on 2016. 5. 20..
 */
public class FrequentItem extends RealmObject {
    @PrimaryKey
    private String primaryKey;
    private String sectionId;
    @Index
    private int slotNumber;
    private String itemId;
    private String title;
    private double money;
    private String leftAccountType;
    private String leftAccountId;
    private String rightAccountType;
    private String rightAccountId;
    private int useCount = 0;
    private long lastUseTime = 0;
    private int sortOrder;

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public String getLeftAccountType() {
        return leftAccountType;
    }

    public void setLeftAccountType(String leftAccountType) {
        this.leftAccountType = leftAccountType;
    }

    public String getLeftAccountId() {
        return leftAccountId;
    }

    public void setLeftAccountId(String leftAccountId) {
        this.leftAccountId = leftAccountId;
    }

    public String getRightAccountType() {
        return rightAccountType;
    }

    public void setRightAccountType(String rightAccountType) {
        this.rightAccountType = rightAccountType;
    }

    public String getRightAccountId() {
        return rightAccountId;
    }

    public void setRightAccountId(String rightAccountId) {
        this.rightAccountId = rightAccountId;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }

    public long getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(long lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void composePrimaryKey() {
        this.primaryKey = sectionId + "|" + slotNumber + "|" + itemId;
    }
}
