package com.zhongjian.util;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 类DateUtil.java的实现描述：时间日期处理工具。
 *
 * @author xin.caix 2015年1月27日 上午9:35:30
 */

public class DateUtil extends DateUtils {
	private static Logger logger = LoggerFactory.getLogger(DateUtil.class);

	/**
	 * "yyyy-MM-dd" 验证
	 */
	public static String PATTERNSTR_YMD = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29)";
	public static String PATTERNSTR_YM = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-((0[1-9])|(1[0-2])))";
	public static String PATTERNSTR_Y = "([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})";
	public static String DateMode_1 = "MM-dd";
	public static String DateMode_2 = "dd";
	public static String DateMode_3 = "HH:mm";
	public static String DateMode_4 = "yyyy-MM-dd HH:mm:ss";
	public static String DateMode_5 = "MM-dd HH:mm";
	public static String DateMode_6 = "yyyyMMdd";
	public static String DateMode_7 = "MM";
	public static SimpleDateFormat date_format = new SimpleDateFormat(DateMode_1);
	public static SimpleDateFormat date_format_str = new SimpleDateFormat(DateMode_6);
	public static SimpleDateFormat datetime_format = new SimpleDateFormat(DateMode_4);
	public static SimpleDateFormat HourMinute = new SimpleDateFormat(DateMode_3);
	public static SimpleDateFormat day = new SimpleDateFormat(DateMode_2);
	public static SimpleDateFormat month = new SimpleDateFormat(DateMode_7);
	public static SimpleDateFormat lastDayTime = new SimpleDateFormat(DateMode_5);
	public static String DATE_REGEX = "\\d{4}-\\d{1,2}-\\d{1,2}";
	public static String DEFAULT_DATE_FORMAT = DateMode_1;

	/**
	 * 格式化制定的date
	 *
	 * @param date
	 * @param simpleDateFormat
	 * @return
	 */
	public synchronized static String formateDate(Date date, SimpleDateFormat simpleDateFormat) {
		if (date == null) {
			return null;
		}
		if (simpleDateFormat == null) {
			return null;
		}
		return simpleDateFormat.format(date);
	}

	/**
	 * 格式化制定的date
	 *
	 * @param date
	 * @param simpleDateFormatStr
	 * @return
	 */
	public static String formateDate(Date date, String simpleDateFormatStr) {
		if (date == null) {
			return null;
		}
		if (simpleDateFormatStr == null) {
			simpleDateFormatStr = DEFAULT_DATE_FORMAT;
		}
		return formateDate(date, new SimpleDateFormat(simpleDateFormatStr));
	}

	/**
	 * 字符串转时间
	 *
	 * @param dateStr
	 * @return
	 */
	public static Date parseStdDate(String dateStr, String simpleDateFormatStr) {

		if (StringUtil.isBlank(dateStr)) {
			return null;
		}
		if (StringUtil.isBlank(simpleDateFormatStr)) {
			return null;
		}
		try {
			return new SimpleDateFormat(simpleDateFormatStr).parse(dateStr);
		} catch (Exception ex) {
			logger.error("parseDate error ", ex);
		}
		return null;
	}

	/**
	 * 字符串转时间
	 *
	 * @param dateStr
	 * @param dateFormat
	 * @return
	 */
	public static Date parseStdDate(String dateStr, DateFormat dateFormat) {

		if (StringUtil.isBlank(dateStr)) {
			return null;
		}
		if (dateFormat == null) {
			return null;
		}
		try {
			return dateFormat.parse(dateStr);
		} catch (Exception ex) {
			logger.error("parseDate error ", ex);
		}
		return null;
	}

	/**
	 * 日期格式化
	 *
	 * @param date
	 * @return
	 */
	public synchronized static String formateDate(Date date) {
		if (date == null) {
			return StringUtil.EMPTY_STRING;
		}
		return date_format.format(date);
	}

	/**
	 * 全时间格式化
	 *
	 * @param date
	 * @return
	 */
	public synchronized static String formateDatetime(Date date) {
		if (date == null) {
			return StringUtil.EMPTY_STRING;
		}

		return datetime_format.format(date);
	}

	/**
	 * 日期加
	 *
	 * @param date
	 * @param days
	 * @return
	 */
	public static Date addDayFroDate(Date date, int days) {
		Calendar c = Calendar.getInstance();
		if (date != null) {
			c.setTime(date);
			c.add(Calendar.DAY_OF_MONTH, days);
		}
		return c.getTime();
	}

	public static Date addHourFroDate(Date date, int hours) {
		Calendar c = Calendar.getInstance();
		if (date != null) {
			c.setTime(date);
			c.add(Calendar.HOUR_OF_DAY, hours);
		}
		return c.getTime();
	}

	/**
	 * 日期减
	 *
	 * @param date
	 * @param days
	 * @return
	 */
	public static Date minusDayForDate(Date date, int days) {
		Calendar c = Calendar.getInstance();
		if (date != null) {
			c.setTime(date);
			c.add(Calendar.DAY_OF_MONTH, -days);
		}
		return c.getTime();
	}

	/**
	 * 添加月
	 *
	 * @param date  日期
	 * @param month 月
	 * @return
	 */
	public static Date addMonthFroDate(Date date, int month) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.MONTH, month);
		return c.getTime();
	}

	/**
	 * 添加年
	 *
	 * @param date 日期
	 * @param year 年
	 * @return
	 */
	public static Date addYearFroDate(Date date, int year) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.YEAR, year);
		return c.getTime();
	}

	/**
	 * 添加年,月
	 *
	 * @param date  日期
	 * @param year  年
	 * @param month 月
	 * @return
	 */
	public static Date addYearAndMonthFroDate(Date date, int year, int month) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.YEAR, year);
		c.add(Calendar.MONTH, month);
		return c.getTime();
	}

	/**
	 * 获取某天的最小时间
	 *
	 * @param date
	 * @return
	 */
	public static Date getDateMin(Date date) {
		try {
			return datetime_format.parse(formateDate(date) + " 00:00:00");
		} catch (Exception ex) {
			logger.error("getDateMin error ", ex);
		}
		return date;
	}

	/**
	 * 获取某天的最大时间
	 *
	 * @param date
	 * @return
	 */
	public static Date getDateMax(Date date) {
		try {
			return datetime_format.parse(formateDate(date) + " 23:59:59");
		} catch (Exception ex) {
			logger.error("getDateMax error ", ex);
		}
		return date;
	}

	/**
	 * 获取一周中的第几天
	 *
	 * @param date
	 * @return
	 */
	public static int getWeekByDate(Date date) {
		Calendar c = Calendar.getInstance();
		if (date != null) {
			c.setTime(date);
		}
		return c.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * 获取相差几天
	 *
	 * @param sd
	 * @param ed
	 * @return
	 */
	public static int getIntervalDayCount(Date sd, Date ed) {
		return (int) ((ed.getTime() - sd.getTime()) / (3600 * 24 * 1000) + 1);
	}

	/**
	 * 两个时间戳相差多少天
	 *
	 * @param sd
	 * @param ed
	 * @return
	 */
	public static int getIntervalDayCount(Long sd, Long ed) {
		return (int) ((ed - sd) / (3600 * 24 * 1000) + 1);
	}

	/**
	 * long型日期转String型
	 *
	 * @param sd
	 * @param isIncludeTime Long型日期是否包含时分秒
	 * @param dateFormat
	 * @return
	 */
	public static String long2Date(Long sd, boolean isIncludeTime, DateFormat dateFormat) {
		if (isIncludeTime == true) {
			return dateFormat.format(new Date(sd));
		} else {
			return dateFormat.format(new Date(sd * 1000));
		}
	}

	/**
	 * 获取明日的字符串
	 *
	 * @param formater
	 * @return
	 */
	public synchronized static String getNextDaystr(SimpleDateFormat formater) {
		if (formater == null) {
			formater = new SimpleDateFormat();
		}
		Date nextDate = addDayFroDate(new Date(), 1);
		return formater.format(nextDate);
	}

	/**
	 * 获取后一天的字符串
	 *
	 * @param dateStr
	 * @param formater
	 * @return
	 */
	public synchronized static String getNextDaystr(String dateStr, SimpleDateFormat formater) {
		if (StringUtil.isBlank(dateStr)) {
			return null;
		}
		if (formater == null) {
			formater = new SimpleDateFormat();
		}
		Date date = null;
		try {
			date = formater.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Date nextDate = addDayFroDate(date, 1);
		return formater.format(nextDate);
	}

	/**
	 * 获取昨日的字符串
	 *
	 * @param formater
	 * @return
	 */
	public synchronized static String getAheadDaystr(SimpleDateFormat formater) {
		if (formater == null) {
			formater = new SimpleDateFormat();
		}
		Date nextDate = addDayFroDate(new Date(), -1);
		return formater.format(nextDate);
	}

	/**
	 * 获取前一天的字符串
	 *
	 * @param dateStr
	 * @return
	 */
	public synchronized static String getAheadDaystr(String dateStr, SimpleDateFormat formater) {
		if (StringUtil.isBlank(dateStr)) {
			return null;
		}
		if (formater == null) {
			formater = new SimpleDateFormat("yyyyMMdd");
		}
		Date date = null;
		try {
			date = formater.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Date nextDate = addDayFroDate(date, -1);
		return formater.format(nextDate);
	}

	/**
	 * 将字符串src按format指定的格式转化为日期对象，本地中文
	 *
	 * @param src    String
	 * @param format String 格式，如yy/MM/dd hh:mm:ss
	 * @return Date对象
	 * @throws ParseException
	 */
	public static Date toDate(String src, String format) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(format, java.util.Locale.CHINESE);
		Date d = null;
		d = sdf.parse(src);
		return d;
	}

	/**
	 * 判断一个字符串是否为符合yyyy-MM-dd格式的时间字符串
	 *
	 * @param str String
	 * @return boolean
	 */
	public static boolean isDate(String str) {
		if (StringUtil.isBlank(str)) {
			return false;
		}

		Pattern p = Pattern.compile(DATE_REGEX);
		if (!p.matcher(str).matches()) {
			return false;
		}

		try {
			toDate(str, DEFAULT_DATE_FORMAT);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 获取年
	 *
	 * @param data
	 * @return
	 * @throws ParseException
	 */
	public static int getYear(String data) throws ParseException {
		if (StringUtil.isBlank(data)) {
			return 0;
		}
		Date startDate = date_format.parse(data);
		Calendar start = Calendar.getInstance();
		start.setTime(startDate);
		return start.get(Calendar.YEAR);
	}

	/**
	 * 获取月
	 *
	 * @param data
	 * @return
	 * @throws ParseException
	 */
	public static int getMonth(String data) throws ParseException {
		if (StringUtil.isBlank(data)) {
			return 0;
		}
		Date endDate = date_format.parse(data);
		Calendar end = Calendar.getInstance();
		end.setTime(endDate);
		return end.get(Calendar.MONTH) + 1;
	}

	/**
	 * 返回两个时间之间的天数
	 *
	 * @param startDay 开始时间
	 * @param endDay   结束时间
	 * @return
	 */
	public static Long getDaysBetweenTwoDays(Date startDay, Date endDay) {

		return (endDay.getTime() - startDay.getTime()) / (1000 * 60 * 60 * 24);
	}

	/**
	 * 返回两个时间之间的天数，endDate - startDate +1
	 *
	 * @param startDay
	 * @param endDay
	 * @return
	 */
	public static Long getDaysMaxBetweenTwoDays(Date startDay, Date endDay) {

		return (endDay.getTime() - startDay.getTime()) / (1000 * 60 * 60 * 24) + 1;
	}

	/**
	 * 获取某个时间段的时间信息
	 *
	 * @param dBegin
	 * @param dEnd
	 * @return
	 */
	public static List<Date> findDates(Date dBegin, Date dEnd) {
		List lDate = new ArrayList();
		lDate.add(dBegin);
		Calendar calBegin = Calendar.getInstance();
		// 使用给定的 Date 设置此 Calendar 的时间
		calBegin.setTime(dBegin);
		Calendar calEnd = Calendar.getInstance();
		// 使用给定的 Date 设置此 Calendar 的时间
		calEnd.setTime(dEnd);
		// 测试此日期是否在指定日期之后
		while (dEnd.after(calBegin.getTime())) {
			// 根据日历的规则，为给定的日历字段添加或减去指定的时间量
			calBegin.add(Calendar.DAY_OF_MONTH, 1);
			lDate.add(calBegin.getTime());
		}
		return lDate;
	}

	/**
	 * 字符串转时间
	 *
	 * @param dateStr
	 * @return
	 */
	public static Date getDate(String dateStr) {
		Date date = new Date();
		try {
			if (Pattern.matches(DateUtil.PATTERNSTR_YMD, dateStr)) {
				SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM-dd");
				date = sf1.parse(dateStr);
			} else if (Pattern.matches(DateUtil.PATTERNSTR_YM, dateStr)) {
				SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM");
				date = sf1.parse(dateStr);
			} else if (Pattern.matches(DateUtil.PATTERNSTR_Y, dateStr)) {
				SimpleDateFormat sf1 = new SimpleDateFormat("yyyy");
				date = sf1.parse(dateStr);
			}
			return date;
		} catch (ParseException e) {
			logger.error("转换时间失败", e);
		}
		return null;
	}

	/**
	 * 判断日期格式:yyyy-mm-dd
	 *
	 * @param sDate
	 * @return
	 */
	public static boolean isValidDate(String sDate) {
		String datePattern1 = "\\d{4}-\\d{2}-\\d{2}";
		String datePattern2 = "^((\\d{2}(([02468][048])|([13579][26]))"
				+ "[\\-\\/\\s]?((((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|"
				+ "(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?"
				+ "((0?[1-9])|([1-2][0-9])))))|(\\d{2}(([02468][1235679])|([13579][01345789]))[\\-\\/\\s]?("
				+ "(((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?"
				+ "((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?((0?[1-9])|(1[0-9])|(2[0-8]))))))";
		if ((sDate != null)) {
			Pattern pattern = Pattern.compile(datePattern1);
			Matcher match = pattern.matcher(sDate);
			if (match.matches()) {
				pattern = Pattern.compile(datePattern2);
				match = pattern.matcher(sDate);
				return match.matches();
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * 是否年月
	 *
	 * @param sDate
	 * @return
	 */

	public static boolean isValidYMDate(String sDate) {
		String datePattern1 = "\\d{4}-\\d{2}";
		String datePattern2 = PATTERNSTR_YM;
		if ((sDate != null)) {
			Pattern pattern = Pattern.compile(datePattern1);
			Matcher match = pattern.matcher(sDate);
			if (match.matches()) {
				pattern = Pattern.compile(datePattern2);
				match = pattern.matcher(sDate);
				return match.matches();
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * 是否年份
	 *
	 * @param sDate
	 * @return
	 */
	public static boolean isValidYDate(String sDate) {
		String datePattern1 = "\\d{4}";
		String datePattern2 = PATTERNSTR_Y;
		if ((sDate != null)) {
			Pattern pattern = Pattern.compile(datePattern1);
			Matcher match = pattern.matcher(sDate);
			if (match.matches()) {
				pattern = Pattern.compile(datePattern2);
				match = pattern.matcher(sDate);
				return match.matches();
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * 获取某个时间所在月的最后一天
	 *
	 * @param date
	 * @return
	 */
	public static Date getLastDayOfMonth(Date date) {
		Date lastDayOfMonth = null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		// 获取某月最大天数
		int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		// 设置日历中月份的最大天数
		cal.set(Calendar.DAY_OF_MONTH, lastDay);
		lastDayOfMonth = cal.getTime();
		return lastDayOfMonth;
	}

	/**
	 * 获取某个时间所在月的第一天
	 *
	 * @param date
	 * @return
	 */
	public static Date getFirstDayOfMonth(Date date) {
		Date firstDayOfMonth = null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		// 设置日历中月份的第一天
		cal.set(Calendar.DAY_OF_MONTH, 1);
		firstDayOfMonth = cal.getTime();
		return firstDayOfMonth;
	}

	/**
	 * 获取某年的最后一天
	 *
	 * @param date
	 * @return
	 */

	public static Date getLastDayOfYear(Date date) {
		Date lastDayOfMonth = null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		// 获取某年最大天数
		int lastDay = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
		// 设置日历中年份的最大天数
		cal.set(Calendar.DAY_OF_YEAR, lastDay);
		lastDayOfMonth = cal.getTime();
		return lastDayOfMonth;
	}

	/**
	 * 获取某年的第一天
	 *
	 * @param date
	 * @return
	 */

	public static Date getFirstDayOfYear(Date date) {
		Date lastDayOfMonth = null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		// 设置日历中年份的最大天数
		cal.set(Calendar.DAY_OF_YEAR, 1);
		lastDayOfMonth = cal.getTime();
		return lastDayOfMonth;
	}

	public static String getDifferenceBetweenTwoDate(Date startTime, Date endTime) {
		/**
		 * 除以1000是为了转换成秒
		 */
		long between = (endTime.getTime() - startTime.getTime()) / 1000;
		long day1 = between / (24 * 3600);
		long hour1 = between % (24 * 3600) / 3600;
		long minute1 = between % 3600 / 60;
		long second1 = between % 60 / 60;
		String str = day1 + "天" + hour1 + "小时" + minute1 + "分" + second1 + "秒";
		return str;
	}

	public static String getMillisecondBetweenTwoDate(Date startTime, Date endTime) {
		long between = (endTime.getTime() - startTime.getTime());
		String str = "相差" + between + "毫秒";
		return str;
	}

	/**
	 * 判断一个时间是否在指定的时间内
	 *
	 * @param date
	 * @param strDateBegin 指定的开始时间 时分秒
	 * @param strDateEnd   指定的结束时间，时分秒
	 * @return
	 */

	public static boolean isInDate(Date date, String strDateBegin, String strDateEnd) {
		SimpleDateFormat sdf = new SimpleDateFormat(DateMode_4);
		String strDate = sdf.format(date);
		//
		// 截取当前时间时分秒
		int strDateH = Integer.parseInt(strDate.substring(11, 13));
		int strDateM = Integer.parseInt(strDate.substring(14, 16));
		int strDateS = Integer.parseInt(strDate.substring(17, 19));
		// 截取开始时间时分秒
		int strDateBeginH = Integer.parseInt(strDateBegin.substring(0, 2));
		int strDateBeginM = Integer.parseInt(strDateBegin.substring(3, 5));
		int strDateBeginS = Integer.parseInt(strDateBegin.substring(6, 8));
		// 截取结束时间时分秒
		int strDateEndH = Integer.parseInt(strDateEnd.substring(0, 2));
		int strDateEndM = Integer.parseInt(strDateEnd.substring(3, 5));
		int strDateEndS = Integer.parseInt(strDateEnd.substring(6, 8));
		if ((strDateH >= strDateBeginH && strDateH <= strDateEndH)) {
			// 当前时间小时数在开始时间和结束时间小时数之间
			if (strDateH > strDateBeginH && strDateH < strDateEndH) {
				return true;
				// 当前时间小时数等于开始时间小时数，分钟数在开始和结束之间
			} else if (strDateH == strDateBeginH && strDateM >= strDateBeginM && strDateM <= strDateEndM) {
				return true;
				// 当前时间小时数等于开始时间小时数，分钟数等于开始时间分钟数，秒数在开始和结束之间
			} else if (strDateH == strDateBeginH && strDateM == strDateBeginM && strDateS >= strDateBeginS
					&& strDateS <= strDateEndS) {
				return true;
			}
			// 当前时间小时数大等于开始时间小时数，等于结束时间小时数，分钟数小等于结束时间分钟数
			else if (strDateH >= strDateBeginH && strDateH == strDateEndH && strDateM <= strDateEndM) {
				return true;
				// 当前时间小时数大等于开始时间小时数，等于结束时间小时数，分钟数等于结束时间分钟数，秒数小等于结束时间秒数
			} else if (strDateH >= strDateBeginH && strDateH == strDateEndH && strDateM == strDateEndM
					&& strDateS <= strDateEndS) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * @return 返回昨日时间
	 */
	public static Date yesterday() {
		Calendar calendar = Calendar.getInstance();
		/**
		 * 设置为前一天
		 */
		calendar.add(Calendar.DATE, -1);
		return calendar.getTime();
	}

	public static Integer getDateInteger(String dateStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		Date date = null;
		try {
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			LogUtil.error(e, "字符日期转换失败");
		}
		return Integer.valueOf(date_format_str.format(date));
	}

	/**
	 * yyyyMMdd转yyyy-MM-dd
	 *
	 * @param dateStr
	 * @return
	 */
	public static String changeDateModel(String dateStr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date date = null;
		try {
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			LogUtil.error(e, "字符日期转换失败");
			return null;
		}
		return date_format.format(date);
	}

	public static long getTodayZeroTime() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		Date zero = calendar.getTime();
		return zero.getTime() / 1000;
	}

	/**
	 * 获取unix时间戳
	 *
	 * @return
	 */
	public static int getCurrentTime() {
		return (int) (System.currentTimeMillis() / 1000);
	}

	/**
	 * 判断时间是否在指定范围
	 *
	 * @return boolean
	 */
	public static boolean belongCalendar(Date nowTime, Date beginTime, Date endTime) {
		Calendar date = Calendar.getInstance();
		date.setTime(nowTime);
		Calendar begin = Calendar.getInstance();
		begin.setTime(beginTime);
		Calendar end = Calendar.getInstance();
		end.setTime(endTime);
		if (date.after(begin) && date.before(end)) {
			return true;
		} else {
			return false;
		}
	}

	public static LocalDateTime getHowManyMinutesAfter(String strDate, Integer minutes) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
		Date date = null;
		try {
			date = simpleDateFormat.parse(strDate);
		} catch (ParseException px) {
		}
		long endTime = date.getTime();
		long finalEndTime = endTime + minutes * 60 * 1000;
		LocalDateTime dateTime =LocalDateTime.ofEpochSecond(finalEndTime/1000,0, ZoneOffset.ofHours(8));
		return dateTime;
	}
	
	public static boolean isBeforeGtAfter(LocalDateTime before,LocalDateTime after) {
		if (before.getHour() > after.getHour()) {
			return true;
		}
		if (before.getHour() == after.getHour() && before.getMinute() > after.getMinute()) {
			return true;
		}
		return false;
	}
	
	public static Integer changeUnixTime(Integer unixTime,String timeStr) {
		String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(unixTime * 1000L);
		String dateTimeStr = dateStr + " " + timeStr;
		Long timeStamp = 0L;
		try {
			timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateTimeStr).getTime();
		} catch (ParseException e) {
		}
		timeStamp =  timeStamp/1000;
		return timeStamp.intValue();
	}
	
}
