package com.kh.semi.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static common.JDBCTemplate.*;

import com.kh.common.util.PageInfo;
import com.kh.semi.api.ApiExamSearchNews;
import com.kh.semi.vo.news;

public class NewsDAO {

	Connection conn = null;
	
	// select
	public List<news> sellectAll(Connection conn, PageInfo pageInfo, String keyword) {

		List<news> newsList = new ArrayList<news>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		if(keyword==null) {
			keyword = "";
		}
		String sql = "SELECT * FROM "
				+ "(SELECT ROWNUM NUM, n.* FROM "
				+ "(SELECT * FROM  NEWS_API WHERE title like ? or description like ?) n) "
				+ "WHERE NUM BETWEEN ? AND ?";
		
		try {

			pstmt = conn.prepareStatement(sql);

				pstmt.setString(1, "%"+keyword+"%");
				pstmt.setString(2, "%"+keyword+"%");
				pstmt.setInt(3, pageInfo.getStartPage());
				pstmt.setInt(4, pageInfo.getEndPage());
				
				rs = pstmt.executeQuery();
			
			while (rs.next()) {
				news news_api = new news();
				news_api.setTitle(rs.getString("title"));
				news_api.setOriginallink(rs.getString("originallink"));
				news_api.setLink(rs.getString("Link"));
				news_api.setDescription(rs.getString("description"));
				news_api.setPubDate(rs.getString("PUBDATE"));

				newsList.add(news_api);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(rs);
			close(pstmt);
			close(conn);
		}

		return newsList;

	}
	public int newsCount(Connection conn) {
		
		int result = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		String sql = "SELECT count(*) FROM  NEWS_API";
		
		try {
			pstmt = conn.prepareStatement(sql);

			rs = pstmt.executeQuery();
			
			rs.next();
			
			result = rs.getInt(1);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(rs);
			close(pstmt);
			close(conn);
		}
		
		return result;
		
	}


	public NewsDAO() {
		try {
			if (conn != null) {
				close(conn);
			}

			this.conn = getConnection();

		} catch (Exception e) {
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close(conn);
	}

	public void passing() {
		deleteAllNewsAPI(); // ????????? ????????? API?????? ???????????? ?????? ?????? ????????? ??????
		String newsJson = ApiExamSearchNews.newsJson(); // ????????? ?????? ?????? ??? ????????????
		String[] passingSplit = newsJson.split("\"items\": \\["); // ??? ??????????????? ??????
		passingSplit = passingSplit[1].split("\\]\\}"); // ??? ??????????????? ??????
		passingSplit = passingSplit[0].split("}"); // ??? ??????????????? ????????? ?????????
		for (int i = 0; i < passingSplit.length; i++) {
			passingSplit[i] = passingSplit[i].replace(",{", "").replace("{", ""); // ?????? ??????
		}
		String[][] rowDataArr = new String[passingSplit.length][]; // ROW???????????? ???????????? 2?????? ??????
		for (int i = 0; i < passingSplit.length; i++) {
			rowDataArr[i] = passingSplit[i].split("\",\""); // ????????? ??????
			for (int j = 0; j < rowDataArr[i].length; j++) {
				rowDataArr[i][j] = rowDataArr[i][j].replaceAll("<b>", "").replaceAll("</b>", "") // DB??? ?????? ????????? ?????? ??????
																									// HTML ??????
						.replaceAll("&quot;", "").strip(); // ???????????? ?????? ??? ??? ??? ?????? ??????
			}
		}

		List<Map<String, String>> jsonList = new ArrayList<Map<String, String>>(); // ROW???????????? ????????? ??????MAP??? ?????? LIST

		for (int i = 0; i < rowDataArr.length; i++) {
			Map<String, String> newsMap = new HashMap<String, String>(); // ROW???????????? ????????? ???
			for (int j = 0; j < rowDataArr[i].length; j++) {
				String[] rowDataInfo = rowDataArr[i][j].split("\":"); // :?????? ????????? ???????????? ???????????? ????????? ?????? ": ??? ?????????

				newsMap.put(rowDataInfo[0].replaceAll("\"", "").strip(), rowDataInfo[1].replaceAll("\"", "").strip()); // ??????
																														// ?????????
																														// ?????????
																														// ??????
			}
			jsonList.add(newsMap); // ??????????????? MAP??? ?????? for?????? ????????? ?????? map??? ???????????? ??????ID??? ????????? ?????? ??????

		}

		for (int i = 0; i < jsonList.size(); i++) {
			String title = jsonList.get(i).get("title");
			String originallink = jsonList.get(i).get("originallink");
			String link = jsonList.get(i).get("link");
			String description = jsonList.get(i).get("description");
			String pubDate = jsonList.get(i).get("pubDate");
			String day = jsonList.get(i).get("pubDate").substring(5, 7).strip();
			String month = jsonList.get(i).get("pubDate").substring(8, 12).toUpperCase().strip();

			if (month.equals("JAN")) {
				month = "1";
			} else if (month.equals("FEB")) {
				month = "2";
			} else if (month.equals("MAR")) {
				month = "3";
			} else if (month.equals("APR")) {
				month = "4";
			} else if (month.equals("MAY")) {
				month = "5";
			} else if (month.equals("JUN")) {
				month = "6";
			} else if (month.equals("JUL")) {
				month = "7";
			} else if (month.equals("AUG")) {
				month = "8";
			} else if (month.equals("SEPT")) {
				month = "9";
			} else if (month.equals("OCT")) {
				month = "10";
			} else if (month.equals("NOV")) {
				month = "11";
			} else if (month.equals("DEC")) {
				month = "12";
			}

			String year = jsonList.get(i).get("pubDate").substring(12, 16);

			pubDate = year + month + day;
			news news = new news(title, originallink, link, description, pubDate);
			System.out.println(news.toString());

			if (insertNewsAPI(news) > 0) {

			} else {
				System.out.println(news.toString());
				System.out.println("??????");
			}

		}
	}

	public int insertNewsAPI(news newsVO) {

		PreparedStatement pstm = null;

		try {
			String sql = "INSERT INTO  NEWS_API VALUES(?,?,?,?,?)";
			pstm = conn.prepareStatement(sql);
			pstm.setString(1, newsVO.getTitle());
			pstm.setString(2, newsVO.getOriginallink());
			pstm.setString(3, newsVO.getLink());
			pstm.setString(4, newsVO.getDescription());
			pstm.setString(5, newsVO.getPubDate());

			int result = pstm.executeUpdate();

			if (result > 0) {
				commit(conn);
			} else {
				rollback(conn);
			}

			return result;

		} catch (Exception e) {
		} finally {
			try {

				if (pstm != null) {
					pstm.close();
				}

			} catch (SQLException e) {
				//
				e.printStackTrace();
			}

		}
		return -1;
	}

	public int deleteAllNewsAPI() {

		PreparedStatement pstm = null;

		try {
			String sql = "DELETE FROM NEWS_API";
			pstm = conn.prepareStatement(sql);

			int result = pstm.executeUpdate();

			if (result > 0) {
				commit(conn);
			} else {
				rollback(conn);
			}

			return result;

		} catch (Exception e) {
		} finally {
			try {

				if (pstm != null) {
					pstm.close();
				}

			} catch (SQLException e) {
				//
				e.printStackTrace();
			}

		}
		return -1;
	}

}
