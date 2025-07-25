package edu.pnu.service.csv;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import edu.pnu.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvSaveBatchService {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * eventhistory 테이블에 맞춘 batch insert
	 */

	public void saveLocations(List<Location> locations) {
	
		if (locations.isEmpty())
			return;
		String sql = "INSERT INTO location (location_id, scan_location, latitude, longitude, operator_id, device_id) VALUES (?, ?, ?, ?, ?, ?)";
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Location l = locations.get(i);
				ps.setLong(1, l.getLocationId());
				ps.setString(2, l.getScanLocation());
				ps.setDouble(3, l.getLatitude());
				ps.setDouble(4, l.getLongitude());
				ps.setLong(5, l.getOperatorId());
				ps.setLong(6, l.getDeviceId());
			}

			public int getBatchSize() {
				return locations.size();
			}
		});
		log.info("[성공] : [CsvSaveBatchService] Location batch insert 완료! 저장 건수: {}", locations.size());
	}

	/** Product batch insert */

	public void saveProducts(List<Product> products) {
		
		
		if (products.isEmpty())
			return;
		String sql = "INSERT INTO product (epc_product, product_name, epc_company) VALUES (?, ?, ?)";
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Product p = products.get(i);
				ps.setString(1, p.getEpcProduct());
				ps.setString(2, p.getProductName());
				ps.setString(3, p.getEpcCompany());
			}

			public int getBatchSize() {
				return products.size();
			}
		});
		log.info("[성공] : [CsvSaveBatchService] Product batch insert 완료! 저장 건수: {}", products.size());
	}

	/** Epc batch insert */

	public void saveEpcs(List<Epc> epcs) {
		
		if (epcs.isEmpty())
			return;
		String sql = "INSERT INTO epc (epc_code, epc_header, epc_lot, epc_serial, location_id, epc_product, manufacture_date, expiry_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Epc e = epcs.get(i);
				ps.setString(1, e.getEpcCode());
				ps.setString(2, e.getEpcHeader());
				ps.setString(3, e.getEpcLot());
				ps.setString(4, e.getEpcSerial());
				ps.setObject(5, e.getLocation() != null ? e.getLocation().getLocationId() : null);
				ps.setObject(6, e.getProduct() != null ? e.getProduct().getEpcProduct() : null);
				
				if (e.getManufactureDate() != null)
					ps.setTimestamp(7, Timestamp.valueOf(e.getManufactureDate()));
				else
					ps.setNull(7, Types.TIMESTAMP);

				// [9] expiry_date
				if (e.getExpiryDate() != null)
					ps.setDate(8, Date.valueOf(e.getExpiryDate()));
				else
					ps.setNull(8, Types.DATE);
			}

			public int getBatchSize() {
				return epcs.size();
			}
		});
		log.info("[성공] : [CsvSaveBatchService] Epc batch insert 완료! 저장 건수: {}", epcs.size());
	}


	/** EventHistory batch insert with KeyHolder - ID를 다시 객체에 설정 */
	public void saveEvent(List<EventHistory> event) {

		
		if (event.isEmpty())
			return;

		String sql = "INSERT INTO eventhistory (epc_code, location_id, hub_type, business_step, business_original, event_type, event_time, file_id, anomaly) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try {
			
//			// KeyHolder 생성 - 생성된 ID들을 받기 위함
//	        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
			
			jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					EventHistory ev = event.get(i);

					// [1] FK: epc_code
					ps.setString(1, ev.getEpc() != null ? ev.getEpc().getEpcCode() : null);

					// [2] FK: location_id
					ps.setObject(2, ev.getLocation() != null ? ev.getLocation().getLocationId() : null);

					// [3] hub_type
					ps.setString(3, ev.getHubType());

					// [4] business_step
					ps.setString(4, ev.getBusinessStep());

					// [5] business_original
					ps.setString(5, ev.getBusinessOriginal());

					// [6] event_type
					ps.setString(6, ev.getEventType());

					// [7] event_time
					if (ev.getEventTime() != null)
						ps.setTimestamp(7, java.sql.Timestamp.valueOf(ev.getEventTime()));
					else
						ps.setNull(7, Types.TIMESTAMP);

					// [10] FK: file_id
					ps.setObject(8, ev.getCsv() != null ? ev.getCsv().getFileId() : null);
					
					ps.setBoolean(9, ev.isAnomaly());
				}

				public int getBatchSize() {
					return event.size();
				}
			});

			log.info("[성공] : [CsvSaveBatchService] EventHistory batch insert 완료! 저장 건수: {}, ID 설정 완료", event.size());    

		}

		catch (Exception e) {
			log.error("[오류] : [CsvSaveBatchService] [EventHistory 저장 실패]", e);
		}
	}
	
}
