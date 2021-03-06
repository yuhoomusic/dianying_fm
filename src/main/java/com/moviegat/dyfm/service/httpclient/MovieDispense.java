package com.moviegat.dyfm.service.httpclient;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.moviegat.dyfm.bean.db.MovieBean;
import com.moviegat.dyfm.bean.db.MovieUrlBean;
import com.moviegat.dyfm.bean.db.UrlExecuteStatBean;
import com.moviegat.dyfm.core.ExecuteUrlResp;
import com.moviegat.dyfm.core.IPDyncDraw;
import com.moviegat.dyfm.dao.MovieDao;
import com.moviegat.dyfm.dao.MovieUrlDao;
import com.moviegat.dyfm.service.htmlparse.IMovieParse;
import com.moviegat.dyfm.service.htmlparse.MovieParse;
import com.moviegat.dyfm.util.RespUrlType;

public class MovieDispense {
	Logger logger = Logger.getLogger(MovieUrlDispense.class);

	// 每次获得总个数
	private int signLoopSize = 20;

	public void getMovie(MovieDao movieDao, MovieUrlDao movieUrlDao,
			IPDyncDraw ipDynDraw) throws Exception {
		Sort sort = new Sort(Direction.DESC, "douban", "imdb", "year");
		long count = movieUrlDao.findIsGatherSize();

		while (count != 0) {
			count = movieUrlDao.findIsGatherSize();
			long loopSizeNum = (count == 0 ? 0 : (count / signLoopSize) + 1);

			for (int loopNum = 0; loopNum < loopSizeNum; loopNum++) {
				logger.info("第 " + (loopNum + 1) + " 次，剩余 --> "
						+ (loopSizeNum - (loopNum + 1)) + " 次");

				Pageable pageable = new PageRequest(loopNum, signLoopSize, sort);

				Page<MovieUrlBean> movieUrlPage = movieUrlDao.findByIsGather(
						false, pageable);

				List<MovieUrlBean> movieUrlList = movieUrlPage.getContent();
				// 构造需要请求的url
				Collection<String> movieLinks = Collections2.transform(
						movieUrlList, new Function<MovieUrlBean, String>() {
							@Override
							public String apply(MovieUrlBean input) {
								return input.getUrl();
							}
						});

				IMovieParse<MovieBean> movieParse = new MovieParse();
				List<UrlExecuteStatBean> urlExecBads = Lists.newArrayList();
				Set<MovieBean> urlResults = Sets.newLinkedHashSet();

				ExecuteUrlResp.doUrlResultByGetMethod(ipDynDraw, movieLinks,
						urlResults, movieParse, urlExecBads, 10,RespUrlType.MOVIE);

				int urlIndex = 0;
				for (MovieBean movie : urlResults) {
					MovieUrlBean movieUrl = movieUrlList.get(urlIndex);
					String url = movieUrl.getUrl();
					Iterable<String> temp = Splitter.on('/').omitEmptyStrings()
							.split(url);

					movie.setDyMovieUrl(Iterables.get(temp, 1));
					movie.setType(StringUtils.trimToEmpty(movieUrl.getType()));

					movieUrl.setIsGather(true);
					urlIndex++;
				}

				movieDao.save(urlResults);
				movieUrlDao.save(movieUrlList);
			}
		}
	}
}