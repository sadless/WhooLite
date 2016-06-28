package com.younggeon.whoolite.realm;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Created by sadless on 2016. 6. 12..
 */
public class Entry extends RealmObject {
    @PrimaryKey
    private String primaryKey;
    private String sectionId;
    private long entryId;
    private String title;
    private double money;
    private String leftAccountType;
    private String leftAccountId;
    private String rightAccountType;
    private String rightAccountId;
    private String memo;
    @Index
    private int entryDate;
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

    public long getEntryId() {
        return entryId;
    }

    public void setEntryId(long entryId) {
        this.entryId = entryId;
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

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public int getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(int entryDate) {
        this.entryDate = entryDate;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void composePrimaryKey() {
        this.primaryKey = sectionId + "|" + entryId;
    }
}
