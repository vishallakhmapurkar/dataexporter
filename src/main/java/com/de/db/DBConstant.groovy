package com.de.db

import com.de.beans.ReportBean;

interface DBConstant {
def REPORT_SQL="SELECT <COLMS> from TC_Report_Gen where reportCode=:reportcode"
def INSNAME="PBSInstance"
def PBS1INSNAME="PBS1"
def PBS2INSNAME="PBS2"
def DATEFORMAT = "MM/dd/yyyy"
def CSV_FILE_EXT = '.csv'
}
