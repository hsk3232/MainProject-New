package edu.pnu.service.statistics;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.pnu.Repo.ProductRepository;
import edu.pnu.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FindAnomalyComponet implements StatisticsInterface {
	
	private final ProductRepository productRepo;
	
	@Override
	public String getProcessorName() {
		return "이상 종류 판별";
	}

	@Override
	public void process(Long fileId) {
		List<Product> p = productRepo
		

	}

	@Override
	public int getOrder() {

		return 0;
	}

}
